# Workflow POC

[TOC]

### Parts of the POC:

1. State-Machine configuration example that covers all the requirements in the workflow SRS.

- ☑️ Can have either a serial or parallel approval flow
- ☑️ Can specify number of reviewers for a workflow type.
- ☑️ Can un-forward a forwarded application in serial flow.
- ☑️ Can have repeated reviewers.
- ☑️ Can specify the max number of times reviewer can request changes to the submitted application before it gets
  rejected.
    - e.g. - (-1: unlimited, 0: never, \>0: upper limit)
- Can have an admin approve, reject or cancel a workflow application, bypassing reviewers.

2. A working state-machine example that models a leave application workflow.
    - using a statemachine builder as per a workflow-type's requirement
    - can create a bean from the statemachine created via builder
    - passing events to a state machine via REST to get desired outcomes.
3. Example to Persist and Fetch a workflow's state-machine context to and from a database.
    - example of saving state-machine context along with an entity.
    - database schema for managing workflows
    - workflow event log with partition tables
4. A state-machine builder code that makes it easy to create any new workflow.
    - a builder class that can be called upon for making any required state-machine when creating a new workflow.

---

<br>

# StateMachine Theory

<br>

**Region**: A Region denotes a behavior fragment that may execute concurrently with its orthogonal Regions.

---

**Vertex**: Vertex is an abstract class that captures the common characteristics for a variety of different concrete
kinds of nodes in the StateMachine graph
(States, Pseudo-states, or ConnectionPointReferences). A Vertex can be the source and/or target of any number of
Transitions.

---

**State**: A State models a situation in the execution of a StateMachine Behavior during which some invariant condition
holds. In most cases this condition is
not explicitly defined, but is implied, usually through the name associated with the State e.g: 'Idle', 'Active'.

**Kinds of State**:

A **simple** State has no internal Vertices or Transitions. <br>
A **composite** State contains at least one Region. <br>
A **submachine** State refers to an entire StateMachine, which is, conceptually, deemed to be “nested” within the State.

---

**Pseudo-State**: A Pseudo-state is an abstraction that encompasses different types of transient Vertices in the
StateMachine graph. Pseudo-states are
generally used to chain multiple Transitions into more complex compound transitions. For example, by combining a
Transition entering a fork Pseudo-state with a
set of Transitions exiting that Pseudo-state, we get a compound Transition that can enter a set of orthogonal Regions.

**Kinds of Pseudo-States**:

**join** – This type of Pseudo-state serves as a common target Vertex for two or more Transitions originating from
Vertices in different orthogonal Regions.
Transitions terminating on a join Pseudo-state cannot have a guard or a trigger. <br>
**fork** – fork Pseudo-states serve to split an incoming Transition into two or more Transitions terminating on Vertices
in orthogonal Regions of a composite
State. The Transitions outgoing from a fork Pseudo-state cannot have a guard or a trigger. <br>
**junction** – This type of Pseudo-state is used to connect multiple Transitions into compound paths between States. For
example, a junction Pseudo-state can be
used to merge multiple incoming Transitions into a single outgoing Transition representing a shared continuation path.
Or, it can be used to split an
incoming Transition into multiple outgoing Transition segments with different guard Constraints. <br>
**choice** – This type of Pseudo-state is similar to a junction Pseudo-state (see above) and serves similar purposes,
with the difference that the guard
Constraints on all outgoing Transitions are evaluated dynamically, when the compound transition traversal reaches this
Pseudo-state. Consequently, choice is
used to realize a dynamic conditional branch. It allows splitting of compound transitions into multiple alternative
paths such that the decision on which
path to take may depend on the results of Behavior executions performed in the same compound transition prior to
reaching the choice point. If more than one
guard evaluates to true, one of the corresponding Transitions is selected. <br>
**terminate** – Entering a terminated Pseudo-state implies that the execution of the StateMachine is terminated
immediately. <br>
**history** - A history state is a pseudo-state, meaning that a state machine can’t rest in a history state. When a
transition that leads to a history state
happens, the history state itself doesn't become active, rather the “most recently visited state” becomes active. It is
a way for a compound state to
remember (when it exits) which state was active, so that if the compound state ever becomes active again, it can go back
to the same active sub-state,
instead of blindly following the initial transition. There are two types of history states, deep history states and
shallow history states. A deep history remembers the deepest active state(s) while a shallow history only remembers the
immediate child’s state.

---

**Transition**:
Three kinds of transitions are defined.

**external**: The Transition exits its source Vertex. If the Vertex is a State, then executing this Transition will
result in the execution of any
associated exit Behavior of that State. <br>
**local**: It is the opposite of external, meaning that the Transition does not exit its containing State (and, hence,
the exit Behavior of the containing
State will not be executed). However, for local Transitions the target Vertex must be different from its source Vertex.
A local Transition can only exist
within a composite State. <br>
**internal**: It is a special case of a local Transition that is a self-transition (i.e., with the same source and
target States), such that the State is never
exited (and, thus, not re-entered), which means that no exit or entry Behaviors are executed when this Transition is
executed. This kind of Transition can
only be defined if the source Vertex is a State. <br>