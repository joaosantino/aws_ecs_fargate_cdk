package com.myorg;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

import java.util.Arrays;

public class AwsEcsFargateCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        VpcStack vpcStack = new VpcStack(app, "Vpc");

        ClusterStack clusterStack = new ClusterStack(app,"Cluster", vpcStack.getVpc());
        clusterStack.addDependency(vpcStack);

        DdbStack ddbStack = new DdbStack(app, "Ddb");

        ServiceStack serviceStack = new ServiceStack(app, "Service", clusterStack.getCluster(), ddbStack.getProductEventsDdb());
        serviceStack.addDependency(clusterStack);
        serviceStack.addDependency(ddbStack);

        app.synth();
    }
}
