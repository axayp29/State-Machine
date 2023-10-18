package com.sttl.hrms.workflow.service;

import com.sttl.hrms.workflow.data.Pair;

import com.sttl.hrms.workflow.data.model.entity.WorkflowInstanceEntity;
import com.sttl.hrms.workflow.exception.WorkflowException;
import com.sttl.hrms.workflow.resource.dto.EventResponseDto;
import com.sttl.hrms.workflow.resource.dto.PassEventDto;
import com.sttl.hrms.workflow.statemachine.EventResultDto;
import com.sttl.hrms.workflow.statemachine.exception.StateMachineException;
import com.sttl.hrms.workflow.statemachine.persist.StateMachineService;
import com.sttl.hrms.workflow.statemachine.util.EventSendHelper;
import com.sttl.hrms.workflow.statemachine.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.statemachine.StateMachine;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.sttl.hrms.workflow.statemachine.SMConstants.*;

@RequiredArgsConstructor
@Slf4j
public abstract class WorkflowService<E extends WorkflowInstanceEntity> {
	private final StateMachineService<WorkflowInstanceEntity> stateMachineService;

	/* CREATE */
	@Transactional
	public E createApplication(JpaRepository<E, Long> repository, E entity, List<PassEventDto> passEvents) {
		validateThatEntityDoesNotExist(entity.getId());

		var stateMachine = stateMachineService.createStateMachine(entity);

		if (passEvents.isEmpty()) {
			stateMachineService.saveStateMachineToEntity(stateMachine, entity, Collections.emptyList(), false);
			var savedEntity = repository.save(entity);
			log.debug("saved entity {}", savedEntity);
			return savedEntity;
		}

		var eventResultVO = EventSendHelper.passEvents(stateMachine, passEvents);
		List<EventResultDto> eventResults = eventResultVO.getSecond();
		if (eventResults.isEmpty())
			throw new WorkflowException("Could not save Application entity",
					new StateMachineException("StateMachine did not accept submit event"));
		stateMachine = eventResultVO.getFirst();

		stateMachineService.saveStateMachineToEntity(stateMachine, entity, eventResults, false);

		var savedEntity = repository.save(entity);
		log.debug("saved entity {}", savedEntity);

		stateMachineService.writeToLog(savedEntity, stateMachine, eventResults);
		return savedEntity;
	}

	/* READ */
	@Transactional(readOnly = true)
	public E getApplicationById(@NotNull Long id, JpaRepository<E, Long> repository) {
		return repository.findById(id).orElseGet(() -> {
			log.warn("No entity found with id: {}", id);
			return null;
		});
	}

	@Transactional(readOnly = true)
	public List<E> getAll(JpaRepository<E, Long> repository) {
		return repository.findAll();
	}

	/* UPDATE */
	@Transactional
	public List<EventResponseDto> passEvent(PassEventDto eventDto, JpaRepository<E, Long> repository) {
		if (eventDto == null) {
			log.warn("returning empty results as there are no events to pass");
			return Collections.emptyList();
		}

		var entity = getApplicationById(eventDto.getWorkflowInstanceId(), repository);
		Long userId = eventDto.getActionBy();

		if (entity == null) {
			log.warn("returning empty results as entity was null");
			return Collections.emptyList();
		}

		var stateMachine = stateMachineService.getStateMachineFromEntity(entity);

		var result = EventSendHelper.passEvent(stateMachine, eventDto);
		var eventResultList = result.getSecond();
		if (eventResultList.isEmpty()) {
			log.warn("the statemachine returned empty results.");
			return Collections.emptyList();
		}

		stateMachine = result.getFirst();

		if (stateMachine.hasStateMachineError()) {
			log.error("Error processing events in statemachine");
			return Collections.emptyList();
		}

		// updates the statemachine state in the respective fields of the entity, as
		// well as logs the event to db if present.
		stateMachineService.saveStateMachineToEntity(stateMachine, entity, eventResultList, true);

		updateApplication(userId, stateMachine, repository, entity);

		return EventResponseDto.fromEventResults(entity.getId(), entity.getTypeId(), eventResultList);
	}

	@Transactional
	public List<EventResponseDto> passEvents(List<PassEventDto> passEvents, JpaRepository<E, Long> repository) {
		if (passEvents == null || passEvents.isEmpty()) {
			log.warn("returning empty results as there are no events to pass");
			return Collections.emptyList();
		}

		Long instanceId = passEvents.get(0).getWorkflowInstanceId();
		Long userId = passEvents.get(0).getActionBy();

		var entity = getApplicationById(instanceId, repository);

		if (entity == null) {
			log.warn("returning empty results as entity was null");
			return Collections.emptyList();
		}

		var stateMachine = stateMachineService.getStateMachineFromEntity(entity);

		var result = EventSendHelper.passEvents(stateMachine, passEvents);
		var eventResultList = result.getSecond();
		stateMachine = result.getFirst();

		if (eventResultList.isEmpty()) {
			log.warn("the statemachine returned empty results.");
			return Collections.emptyList();
		}

		if (stateMachine.hasStateMachineError()) {
			log.error("Error processing events in statemachine");
			return Collections.emptyList();
		}

		// updates the statemachine state in the respective fields of the entity, as
		// well as logs the event to db if present.
		stateMachineService.saveStateMachineToEntity(stateMachine, entity, eventResultList, true);

		var savedEntity = updateApplication(userId, stateMachine, repository, entity);
		log.debug("Updated entity with the following delta changes: {}", StringUtil.beanDelta(entity, savedEntity));

		return EventResponseDto.fromEventResults(entity.getId(), entity.getTypeId(), eventResultList);
	}

	@Transactional
	public E updateApplication(Long userId, StateMachine<String, String> stateMachine,
			JpaRepository<E, Long> repository, E entity) {

		var map = stateMachine.getExtendedState().getVariables();

		// update entity details
		entity.setUpdatedDate(LocalDateTime.now());
		entity.setUpdatedByUserId(userId);
		entity.setCurrentState(stateMachine.getState().getId());
		Optional.ofNullable(map.get(KEY_FORWARDED_COUNT)).map(fwdCount -> ((Integer) fwdCount).shortValue())
				.filter(forwardCount -> entity.getForwardCount() != forwardCount).ifPresent(entity::setForwardCount);
		Optional.ofNullable(map.get(KEY_RETURN_COUNT)).map(returnCount -> ((Integer) returnCount).shortValue())
				.filter(returnCount -> entity.getTimesReturnedCount() != returnCount)
				.ifPresent(entity::setTimesReturnedCount);
		Optional.ofNullable(map.get(KEY_ROLL_BACK_COUNT)).map(rollBackCount -> ((Integer) rollBackCount).shortValue())
				.filter(rollBackCount -> entity.getTimesRolledBackCount() != rollBackCount)
				.ifPresent(entity::setTimesRolledBackCount);
		Optional.ofNullable(map.get(KEY_FORWARDED_BY_LAST))
				.map(lastForwardBy -> ((Pair<Integer, Long>) lastForwardBy).getSecond())
				.filter(lastForwardBy -> !ObjectUtils.nullSafeEquals(lastForwardBy, entity.getLastForwardedBy()))
				.ifPresent(entity::setLastForwardedBy);

		// update entity
		var updatedEntity = repository.save(entity);
		log.debug("Updated Entity: {}", updatedEntity);
		return updatedEntity;
	}

	abstract boolean existsById(Long id);

	@Transactional
	public List<EventResultDto> resetStateMachine(PassEventDto passEventDto, JpaRepository<E, Long> repository) {
		var entity = getApplicationById(passEventDto.getWorkflowInstanceId(), repository);
		var stateMachine = stateMachineService.getStateMachineFromEntity(entity);
		var eventResultList = stateMachineService.resetStateMachine(entity, passEventDto, stateMachine);
		var savedEntity = repository.save(entity);
		stateMachineService.saveStateMachineToEntity(stateMachine, savedEntity, eventResultList, true);
		return eventResultList;
	}

	/* DELETE */
	@Transactional
	public void deleteApplication(@NotNull Long id, JpaRepository<E, Long> repository) {
		repository.deleteById(id);
		log.debug("deleted entity with id: {}", id);
	}

	/* OTHER METHODS */
	private void validateThatEntityDoesNotExist(@NotNull Long id) {
		if (id != null && existsById(id)) {
			throw new WorkflowException("Cannot save LeaveAppWorkflowInstance. An entry with this id already exists");
		}
	}

}
