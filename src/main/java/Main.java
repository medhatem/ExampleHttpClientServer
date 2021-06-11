import service.DomainNameService;
import service.HttpService;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String... args) throws IOException {
        Scanner scanner;
        String domainName, ipAddress;

        DomainNameService domainNameService = new DomainNameService();
        HttpService httpService;

        System.out.println("IFT585 - TP3");
        System.out.println("By Christophe Pigeon, Marc-Antoine Dugal & Mohamed Hatem Diabi");

        scanner = new Scanner(System.in);

        // Step 1: Get Domain Name & validate it
        System.out.println("Enter Domain Name: ");
        domainName = scanner.nextLine();

        while (!DomainNameService.validateDomainName(domainName)) {
            System.out.println("Invalid Domain Name. Please retry: ");
            domainName = scanner.nextLine();
        }

        // Step 2: Get IP address of Domain Name
        ipAddress = domainNameService.getIpAddress(domainName);
        System.out.println("IP Address: " + ipAddress);

        // ipAdresse Validation
        if(!DomainNameService.validationIPAddress(ipAddress)){
            System.err.println("Unexpected Error :: This website is not an IPV4 address");
            return;
        }

        // Step 3: Send request to HTTP server, display response & download images if any
        httpService = new HttpService(ipAddress, domainName);
        httpService.displayHttpInfo();
    }
}