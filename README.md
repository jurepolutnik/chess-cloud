# chess-cloud

Chess-cloud is a prototype system for analysing chess posisitons in cloud environment using [StockFish5](https://stockfishchess.org/) chess engine (oen of the best chess eninges that is also open-sourced). Using [Akka Cluster](http://doc.akka.io/docs/akka/2.2.0/common/cluster.html), work (chess analysis) is
distributed among available workers runnign on different computing nodes. As a foundation pattern described in blog post [Balancing Workload Across Nodes with Akka 2](http://letitcrash.com/post/29044669086/balancing-workload-across-nodes-with-akka-2) was used.

Chess cluster is accessible through web application writen in AngularJS and PlayFramework. Frontend provides simple and interactive user interface, which allows 
  - Playing Chess game with Computer (Cluster Engine) 
  - Analysing chess games 
    - supports PGN (Portable Game Notation)
    - Play-out current position 
  - Small DB of one-of-the-best chess games ever played 


Running instance can be found at [Chess-cloud.com](http://www.chess-cloud.com).

## Running (Dev mode)

Frontend: `cd frontend/ && sbt run`

Cluster:  `cd cluster/ && sbt run`

Open [localhost:9000](http://localhost:9000)

This commands runs frontend web application and cluster in a development mode. Cluster is made of 1 master available on localhost:2551, 2 workers on random ports and a frontend that is run as part of frontend application in separate actor system. 


## Deploying

Deployment is done using [Fabric](http://www.fabfile.org/) scripts and [LibCloud](http://libcloud.readthedocs.org/en/latest/). Atm, deployment is possible
to AWS and OpenStack, however adding additional provider should be fairly easy (extending scripts/driver.py). 

#### Prerequisite
Python, VirtualEnv, Pip > Set up python virtual environment and install requirements.

`chesscloud-deps` image available in target cloud enviornment. It can be default image, where
provision is run (installs Java8) on created nodes. 

#### Configuration

Configuration file `conf.ini` holds cloud provider authentication, target infrastructure properties (VM sizes), and
name of prepared image `chesscloud-deps`, which should be prepared in advance and will be used when spwning new VMs.

#### Commands (fabric)
```
# Creating and deploying cluster
fab create_cluster 
fab deploy_cluster

# Scaling up and down (argument: cluster_node=[frontend|master|worker])
fab scale_up
fab scale_down

# Listing nodes - or selection
fab frontends | masters | workers
fab cluster | all

# Starting and stopping cluster or selected nodes
fab stop_cluster | start_cluster
fab <selection> start|stop

# Destroying cluster or selected nodes
fab destroy_cluster
fab <slection> destroy

# Misc commands - ssh to machine, execute cmd on target machine, ...
fab <selection> ssh | cmd get_log | tail_log

```
