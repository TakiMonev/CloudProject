package aws;

/*
 * Cloud Computing
 *
 * Dynamic Resource Management Tool
 * using AWS Java SDK Library
 *
 */
import java.io.BufferedReader;
import java.util.Collections;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.io.InputStream;
import java.util.Scanner;
import java.time.LocalDate;
import java.util.List;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalAdjusters;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.costexplorer.model.*;
import com.amazonaws.services.costexplorer.AWSCostExplorer;
import com.amazonaws.services.costexplorer.AWSCostExplorerClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.DryRunSupportedRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.RebootInstancesRequest;
import com.amazonaws.services.ec2.model.RebootInstancesResult;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.*;

public class awsTest {

    static AmazonEC2      ec2;
    static ProfileCredentialsProvider credentialsProvider;
    static String masterId = "i-0560c4b836b88b949";
    static AWSSimpleSystemsManagement ssm;

    private static void init() throws Exception {
        ssm = AWSSimpleSystemsManagementClientBuilder.defaultClient();
        ec2 = AmazonEC2ClientBuilder.defaultClient();

        credentialsProvider = new ProfileCredentialsProvider();
        try {
            credentialsProvider.getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (C:\\Users\\이상윤\\.aws\\credentials), and is in valid format.",
                    e);
        }
        ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion("ap-northeast-2")
                .build();
    }

    public static void main(String[] args) throws Exception {

        init();

        Scanner menu = new Scanner(System.in);
        Scanner id_string = new Scanner(System.in);
        int number = 0;

        while(true)
        {
            System.out.println("                                                            ");
            System.out.println("                                                            ");
            System.out.println("------------------------------------------------------------");
            System.out.println("           Amazon AWS Control Panel using SDK               ");
            System.out.println("------------------------------------------------------------");
            System.out.println("  1. list instance                2. available zones        ");
            System.out.println("  3. start instance               4. available regions      ");
            System.out.println("  5. stop instance                6. create instance        ");
            System.out.println("  7. reboot instance              8. list images            ");
            System.out.println("  9. predict costs               10. display IP address     ");
            System.out.println(" 11. condor_q status                             ");
            System.out.println("                                  99. quit                  ");
            System.out.println("------------------------------------------------------------");

            System.out.print("Enter an integer: ");

            if(menu.hasNextInt()){
                number = menu.nextInt();
            }else {
                System.out.println("concentration!");
                break;
            }


            String instance_id = "";

            switch(number) {
                case 1:
                    listInstances();
                    break;

                case 2:
                    availableZones();
                    break;

                case 3:
                    System.out.print("Enter instance id: ");
                    if(id_string.hasNext())
                        instance_id = id_string.nextLine();

                    if(!instance_id.isBlank())
                        startInstance(instance_id);
                    break;

                case 4:
                    availableRegions();
                    break;

                case 5:
                    System.out.print("Enter instance id: ");
                    if(id_string.hasNext())
                        instance_id = id_string.nextLine();

                    if(!instance_id.isBlank())
                        stopInstance(instance_id);
                    break;

                case 6:
                    System.out.print("Enter ami id: ");
                    String ami_id = "";
                    if(id_string.hasNext())
                        ami_id = id_string.nextLine();

                    if(!ami_id.isBlank())
                        createInstance(ami_id);
                    break;

                case 7:
                    System.out.print("Enter instance id: ");
                    if(id_string.hasNext())
                        instance_id = id_string.nextLine();

                    if(!instance_id.isBlank())
                        rebootInstance(instance_id);
                    break;

                case 8:
                    listImages();
                    break;

                case 9:
                    System.out.println("Costs until yesterday :");
                    predictCosts();
                    break;

                case 10:
                    displayIPAddress(masterId);
                    break;

                case 11:
                    executeCommand(masterId, "condor_q");
                    break;

                case 99:
                    System.out.println("bye!");
                    menu.close();
                    id_string.close();
                    return;
                default: System.out.println("wrong option!");
            }

        }

    }

    public static void executeCommand(String instanceId, String command) {
        System.out.println("Executing command on instance " + instanceId + ": " + command);

        SendCommandRequest sendCommandRequest = new SendCommandRequest()
                .withInstanceIds(instanceId)
                .withDocumentName("AWS-RunShellScript")
                .withParameters(Collections.singletonMap("commands", Collections.singletonList(command)));

        SendCommandResult sendCommandResult = ssm.sendCommand(sendCommandRequest);
        String commandId = sendCommandResult.getCommand().getCommandId();

        GetCommandInvocationRequest getCommandInvocationRequest = new GetCommandInvocationRequest()
                .withCommandId(commandId)
                .withInstanceId(instanceId);

        GetCommandInvocationResult commandInvocationResult = null;
        boolean commandExecuted = false;
        while (!commandExecuted) {
            try {
                Thread.sleep(5000);
                commandInvocationResult = ssm.getCommandInvocation(getCommandInvocationRequest);
                if (commandInvocationResult.getStatus().equals("Success")) {
                    commandExecuted = true;
                }
            } catch (InterruptedException | AmazonServiceException e) {
                e.printStackTrace();
            }
        }

        if (commandInvocationResult != null) {
            System.out.println("Command output: " + commandInvocationResult.getStandardOutputContent());
        }
    }


    public static void listInstances() {

        System.out.println("Listing instances....");
        boolean done = false;

        DescribeInstancesRequest request = new DescribeInstancesRequest();

        while(!done) {
            DescribeInstancesResult response = ec2.describeInstances(request);

            for(Reservation reservation : response.getReservations()) {
                for(Instance instance : reservation.getInstances()) {
                    System.out.printf(
                            "[id] %s, " +
                                    "[AMI] %s, " +
                                    "[type] %s, " +
                                    "[state] %10s, " +
                                    "[monitoring state] %s",
                            instance.getInstanceId(),
                            instance.getImageId(),
                            instance.getInstanceType(),
                            instance.getState().getName(),
                            instance.getMonitoring().getState());
                }
                System.out.println();
            }

            request.setNextToken(response.getNextToken());

            if(response.getNextToken() == null) {
                done = true;
            }
        }
    }

    public static void availableZones()    {

        System.out.println("Available zones....");
        try {
            DescribeAvailabilityZonesResult availabilityZonesResult = ec2.describeAvailabilityZones();
            Iterator <AvailabilityZone> iterator = availabilityZonesResult.getAvailabilityZones().iterator();

            AvailabilityZone zone;
            while(iterator.hasNext()) {
                zone = iterator.next();
                System.out.printf("[id] %s,  [region] %15s, [zone] %15s\n", zone.getZoneId(), zone.getRegionName(), zone.getZoneName());
            }
            System.out.println("You have access to " + availabilityZonesResult.getAvailabilityZones().size() +
                    " Availability Zones.");

        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }

    }

    public static void startInstance(String instance_id)
    {

        System.out.printf("Starting .... %s\n", instance_id);
        final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

        DryRunSupportedRequest<StartInstancesRequest> dry_request =
                () -> {
                    StartInstancesRequest request = new StartInstancesRequest()
                            .withInstanceIds(instance_id);

                    return request.getDryRunRequest();
                };

        StartInstancesRequest request = new StartInstancesRequest()
                .withInstanceIds(instance_id);

        ec2.startInstances(request);

        System.out.printf("Successfully started instance %s", instance_id);
    }


    public static void availableRegions() {

        System.out.println("Available regions ....");

        final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

        DescribeRegionsResult regions_response = ec2.describeRegions();

        for(Region region : regions_response.getRegions()) {
            System.out.printf(
                    "[region] %15s, " +
                            "[endpoint] %s\n",
                    region.getRegionName(),
                    region.getEndpoint());
        }
    }

    public static void stopInstance(String instance_id) {
        final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

        DryRunSupportedRequest<StopInstancesRequest> dry_request =
                () -> {
                    StopInstancesRequest request = new StopInstancesRequest()
                            .withInstanceIds(instance_id);

                    return request.getDryRunRequest();
                };

        try {
            StopInstancesRequest request = new StopInstancesRequest()
                    .withInstanceIds(instance_id);

            ec2.stopInstances(request);
            System.out.printf("Successfully stop instance %s\n", instance_id);

        } catch(Exception e)
        {
            System.out.println("Exception: "+e.toString());
        }

    }

    public static void createInstance(String ami_id) {
        final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

        RunInstancesRequest run_request = new RunInstancesRequest()
                .withImageId(ami_id)
                .withInstanceType(InstanceType.T2Micro)
                .withMaxCount(1)
                .withMinCount(1);

        RunInstancesResult run_response = ec2.runInstances(run_request);

        String reservation_id = run_response.getReservation().getInstances().get(0).getInstanceId();

        System.out.printf(
                "Successfully started EC2 instance %s based on AMI %s",
                reservation_id, ami_id);

    }

    public static void rebootInstance(String instance_id) {

        System.out.printf("Rebooting .... %s\n", instance_id);

        final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

        try {
            RebootInstancesRequest request = new RebootInstancesRequest()
                    .withInstanceIds(instance_id);

            RebootInstancesResult response = ec2.rebootInstances(request);

            System.out.printf(
                    "Successfully rebooted instance %s", instance_id);

        } catch(Exception e)
        {
            System.out.println("Exception: "+e.toString());
        }


    }

    public static void listImages() {
        System.out.println("Listing images....");

        final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

        DescribeImagesRequest request = new DescribeImagesRequest();
        ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();

        request.getFilters().add(new Filter().withName("name").withValues("htcondor-slave-image"));
        request.setRequestCredentialsProvider(credentialsProvider);

        DescribeImagesResult results = ec2.describeImages(request);

        for(Image images :results.getImages()){
            System.out.printf("[ImageID] %s, [Name] %s, [Owner] %s\n",
                    images.getImageId(), images.getName(), images.getOwnerId());
        }

    }

    public static void predictCosts() {
        AWSCostExplorer awsCostExplorerClient;

        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate;

        if (today.getDayOfMonth() == 1) {
            startDate = today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
            endDate = today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        } else {
            startDate = today.with(TemporalAdjusters.firstDayOfMonth());
            endDate = today.minusDays(1);
        }

        AWSCostExplorerClientBuilder builder = AWSCostExplorerClientBuilder.standard();

        awsCostExplorerClient = AWSCostExplorerClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion("ap-northeast-2")
            .build();

        GetCostAndUsageRequest request = new GetCostAndUsageRequest()
                .withTimePeriod(new DateInterval().withStart(startDate.toString()).withEnd(endDate.toString()))
                .withGranularity("MONTHLY")
                .withMetrics("BlendedCost");

        GetCostAndUsageResult result = awsCostExplorerClient.getCostAndUsage(request);

        result.getResultsByTime().forEach(resultByTime -> {
            System.out.println(resultByTime.toString());
        });

        awsCostExplorerClient.shutdown();
    }

    public static void displayIPAddress(String instance_id) {
        System.out.println("Displaying IP Address....");
        DescribeInstancesRequest request = new DescribeInstancesRequest()
                .withInstanceIds(instance_id);

        DescribeInstancesResult response = ec2.describeInstances(request);

        for (Reservation reservation : response.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                System.out.printf("[Instance ID] %s, [IP Address] %s\n",
                        instance.getInstanceId(), instance.getPublicIpAddress());
            }
        }
    }
}