package com.myorg;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;

import java.util.HashMap;
import java.util.Map;

public class ServiceStack extends Stack {
    public ServiceStack(final Construct scope, final String id, Cluster
            cluster, Table table) {
        this(scope, id, null, cluster, table);
    }

    public ServiceStack(final Construct scope, final String id,
                        final StackProps props, Cluster cluster, Table table) {
        super(scope, id, props);
        // The code that defines your stack goes here
        CfnParameter awsRegion = CfnParameter.Builder.create(this, "awsRegion")
                .type("String")
                .description("The AWS Region")
                .build();

        Map<String, String> envVariables = new HashMap<>();
        envVariables.put("AWS_REGION", awsRegion.getValueAsString());

        LogDriver logDriver = LogDriver.awsLogs(AwsLogDriverProps.builder()
                .logGroup(LogGroup.Builder.create(this, "Service01LogGroup")
                        .logGroupName("Service01")
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .build())
                .streamPrefix("Service01")
                .build());

        ApplicationLoadBalancedTaskImageOptions task =
                ApplicationLoadBalancedTaskImageOptions.builder()
                        .containerName("ddb_sqs_demo")
                        .image(ContainerImage.fromRegistry("siecola/aws-ddb-sqs-java-demo:1.0.0"))
                        .containerPort(8080)
                        .logDriver(logDriver)
                        .environment(envVariables)
                        .build();

        ApplicationLoadBalancedFargateService service01 =
                ApplicationLoadBalancedFargateService.Builder
                        .create(this, "ALB01")
                        .serviceName("service-01")
                        .cluster(cluster)
                        .cpu(512)
                        .memoryLimitMiB(1024)
                        .desiredCount(2)
                        .listenerPort(8080)
                        .taskImageOptions(task)
                        .publicLoadBalancer(true)
                        .build();

        service01.getTargetGroup().configureHealthCheck(new HealthCheck.Builder()
                .path("/actuator/health")
                .port("8080")
                .healthyHttpCodes("200")
                .build());

        ScalableTaskCount scalableTaskCount = service01.getService()
                .autoScaleTaskCount(EnableScalingProps.builder()
                        .minCapacity(2)
                        .maxCapacity(4)
                        .build());

        scalableTaskCount.scaleOnCpuUtilization("Service01AutoScaling",
                CpuUtilizationScalingProps.builder()
                        .targetUtilizationPercent(50)
                        .scaleInCooldown(Duration.seconds(60))
                        .scaleOutCooldown(Duration.seconds(60))
                        .build());

        table.grantReadWriteData(service01.getTaskDefinition().getTaskRole());

    }
}
