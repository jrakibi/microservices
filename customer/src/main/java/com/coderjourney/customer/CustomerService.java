package com.coderjourney.customer;

import com.coderjourney.amqp.RabbitMQMessageProducer;
import com.coderjourney.clients.fraud.FraudCheckResponse;
import com.coderjourney.clients.fraud.FraudClient;
import com.coderjourney.clients.notification.NotificationClient;
import com.coderjourney.clients.notification.NotificationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CustomerService {
    @Autowired
    CustomerRepository customerRepository;
    @Autowired
    FraudClient fraudClient;
    @Autowired
    NotificationClient notificationClient;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    RabbitMQMessageProducer rabbitMQMessageProducer;

    public void registerCustomer(CustomerRegistrationRequest request) {
        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .build();
        customerRepository.saveAndFlush(customer);

//        FraudCheckResponse fraudCheckResponse = restTemplate.getForObject(
//                "http://FRAUD/api/v1/fraud-check/{customerId}",
//                FraudCheckResponse.class,
//                customer.getId()
//        );

        FraudCheckResponse fraudCheckResponse = fraudClient.isFraudster(customer.getId());

        if(fraudCheckResponse.isFraudster()) {
            throw new IllegalStateException("Fraudster");
        }


        NotificationRequest notificationRequest =
                new NotificationRequest(
                    customer.getId(),
                    customer.getEmail(),
                    String.format("Hi %s, Welcome to coder.journey community ..", customer.getFirstName())
                );

        rabbitMQMessageProducer.publish(
                notificationRequest,
                "internal.exchange",
                "internal.notification.routing-key"

        );

//        notificationClient.sendNotification(
//                new NotificationRequest(
//                    customer.getId(),
//                    customer.getEmail(),
//                    String.format("Hi %s, Welcome to coder.journey community ..", customer.getFirstName()
//                    )
//        ));


    }
}
