package it.polito.ai.project.server.tasks;

import it.polito.ai.project.server.dtos.DeliveryDTO;
import it.polito.ai.project.server.dtos.StudentDTO;
import it.polito.ai.project.server.entities.Delivery;
import it.polito.ai.project.server.repositories.AssignmentRepository;
import it.polito.ai.project.server.repositories.DeliveryRepository;
import it.polito.ai.project.server.services.GeneralService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Calendar;

@Component
@EnableAsync
public class ExpiredDeliveriesTask {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private GeneralService generalService;

    @Autowired
    ModelMapper modelMapper;

    @Async
    @Scheduled(fixedRate = 86400000)
    public void checkExpiredHomework() throws InterruptedException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Calendar c = Calendar.getInstance();

        // get the time to sleep until the midnight
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        // sleep until midnight
        Thread.sleep((c.getTimeInMillis() - System.currentTimeMillis()));

        this.assignmentRepository.findAll()
                .stream()
                .filter(x -> x.getExpiryDate().getTime() < System.currentTimeMillis())
                .flatMap(x -> x.getHomeworks().stream())
                .filter(x -> (
                    this.generalService
                            .getAssignmentLastDelivery(x.getAssignment().getId(),
                                                        x.getStudent().getId())
                            .getStatus()
                            .equals(Delivery.Status.READ) |
                    this.generalService
                            .getAssignmentLastDelivery(x.getAssignment().getId(),
                                                        x.getStudent().getId())
                            .getStatus()
                            .equals(Delivery.Status.NULL)))
                .forEach(x -> {
                    // create a delivery with DELIVERED status
                    // for each student that did not delivered anything
                    Delivery delivery = new Delivery();
                    delivery.setStatus(Delivery.Status.DELIVERED);
                    delivery.setTimestamp(timestamp);
                    delivery.setHomework(x);
                    delivery.setPathImage("src/main/resources/images/deliveries/empty_image.png");
                    this.deliveryRepository.save(delivery);
                    x.setEditable(false);
                } );

    }

}
