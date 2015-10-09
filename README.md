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
- Python, VirtualEnv, Pip > Set up python virtual environment and install requirements.
- 

#### Configuration

Configuration file `conf.ini`. 

`cd deploy/ && fab create_cluster && fab deploy_cluster`
