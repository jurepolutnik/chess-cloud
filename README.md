# chess-cloud

Chess-cloud is a prototype system for analysing chess posisitons in cloud environment using [StockFish5](https://stockfishchess.org/) chess engine (oen of the best chess eninges that is also open-sourced). Using [Akka Cluster](http://doc.akka.io/docs/akka/2.2.0/common/cluster.html), work (chess analysis) is
distributed among available workers runnign on different computing nodes. As a foundation pattern described in blog post [Balancing Workload Across Nodes with Akka 2](http://letitcrash.com/post/29044669086/balancing-workload-across-nodes-with-akka-2) was used.

Chess cluster is accessible through web application writen in AngularJS and PlayFramework. Frontend provides simple and interactive user interface, which allows 
  - Playing Chess game with Computer (Cluster Engine) 
  - Analysing chess games 
    - supports PGN (Portable Game Notation)
    - Play-out current position 
  - Small DB of one-of-the-best chess games ever played 


Running instance can be found at [Chess-cloud](http://www.chess-cloud.com).

## Running

Frontend: `cd frontend/; sbt run`

Cluster:  `cd cluster/; sbt run`

## Deploying

`cd deploy/ && fab create_cluster && fab deploy_cluster`
