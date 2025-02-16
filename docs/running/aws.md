---
layout: default
title: Running on AWS
parent: Running Zingg on Cloud
nav_order: 5
---
## Running on AWS Elastic Map Reduce

One option is to use the spark-submit option with the Zingg config and phase. 

    aws emr create-cluster --name "Add Spark Step Cluster" --release-label emr-6.2.0 --applications Name=Zingg \
    --ec2-attributes KeyName=myKey --instance-type <instance type> --instance-count <num instances> \
    --steps Type=Spark,Name="Zingg",ActionOnFailure=CONTINUE,Args=[--class,zingg.client.Client,<s3 location of zingg.jar>,--phase,<name of phase - findTrainingData,match etc>,--conf,<s3 location of config.json>] --use-default-roles````

A second option is to run Zingg Python code in [AWS EMR Notebooks](https://aws.amazon.com/emr/features/notebooks/)
