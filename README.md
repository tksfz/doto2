Doto - task planning for teams
==============================

Doto is a collaborative task planner with the following features:
- Distributed, using git for storage and sync; works offline
- Hierarchical, representing work as an arbitrarily deep tree
- Support for more complex workflows using _threads_ and _events_

Installing Doto
---------------

To install doto on Mac OS do:

```
$ brew install tksfz/doto/dotodoto
```

After installation you can create a new project and create your first task with:

```
$ doto new myproject
$ doto ls
94e4cf ~~ root
         Scheduled:
         Unscheduled:
$ doto add -p 94e4c "my first task"
dcc73c [ ] my first task
```

Experiment with the `dotodoto` repo
-----------------------------------
`dotodoto`[1] is the doto repo for doto development. Doto uses git for storage and syncing. To get `dotodoto` do:

```
$ doto get git@github.com:tksfz/dotodoto.git
```

will clone `dotodoto` into a directory underneath your DOTO_HOME (`~/.doto`) and set it as your active project. (Note that only ssh URL's - not HTTPS URL's - are fully supported at the moment.) Unlike git, doto doesn't determine the active project based on your current directory. Instead the `doto project` command lets you set the active project.

Now run `doto ls` to see all tasks in the `dotodoto` repo.

Use `doto help` to see a list of all commands. The most useful commands are `doto add` to add a task or event; `doto thread` to create a thread; and `doto sync` to sync changes to your doto project with the remote.

Doto in a nutshell
------------------

*Tasks.* Tasks are listed with a checkbox `[ ]`. Everyone knows what tasks are. That's all the work you do, the product you build, the designs you make, the documentation you write, the bugs you find and fix, etc. Tasks are _divisible_ into sub-tasks.

*Threads.* Threads (indicated with `~~`) are a grouping of tasks that all make progress toward the same goal. Threads are purely organizational. They don't themselves represent work to be done; they organize work into logical threads. Threads are useful in a variety of situations: for separating feature-building work from maintenance work; for distinguishing work on the "critical path" vs work that's off that path; etc.

Threads can have sub-threads, in addition to having tasks. Everything in a doto repo lives under a root thread.

*Events.* Events (`![ ]`) map roughly to product code deployments. Events live within event threads (`~~!`), which lay out a sequence of deployments. The planning process in doto involves attaching tasks to events. That is, saying what will be deployed when, with an understanding of what effects should occur as a result. By compelling users to specify up-front what gets deployed in what order, doto encourages early and frequent deployments, and iterative development.

Creating and pushing a new doto project
---------------------------------------

To create a new doto project, use `doto new <project name>`. To push this project to a central git repo, you'll need to:
- Create the repo on, say, github.com
- Run `doto sync -n -r <remote ssh url>` for the initial push to the remote
- Run `doto sync` to sync changes to/from the remote

Others can then fetch your doto project using `doto get <remote ssh url>`. Access is governed by the usual git ssh access controls.

[1]: https://github.com/tksfz/dotodoto
