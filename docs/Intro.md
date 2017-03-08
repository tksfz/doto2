


The Tree of Work
Tasks
Threads
Events



Events

Events are the cornerstone of doto2.

Something "has to happen" for something to be an event.

An event changes the state of the world and it changes the "equilibrium".

Common Events

Three common kinds of events in software engineering are: deployments, feature gate enablements,
and demos.

Events are coordination points.

Events Form a Graph

Events exist in a graph. Up to now, we've been discussing the tree of work as just that - a tree. But, in general, events don't fit within a tree. Take the typical example of a deployment artifact: Multiple threads of work can be flowing changes into the same artifact. The artifact is deployed with all those changes, from multiple threads of work - the physical nature of the artifact forces this.

If events don't fit within the tree of work that we've discussed so far, where should they live?
