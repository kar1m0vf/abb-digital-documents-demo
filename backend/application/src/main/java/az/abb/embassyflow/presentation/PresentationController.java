package az.abb.embassyflow.presentation;

import az.abb.embassyflow.common.web.AuthAttributes;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/** UI compatibility endpoints, available only in the local presentation profile. */
@RestController
@Profile("presentation")
@RequestMapping("/api/v1/demo")
public class PresentationController {
    private final PresentationService service;
    public PresentationController(PresentationService service) { this.service=service; }
    @GetMapping("/health") public Map<String,String> health() { return Map.of("mode","presentation","status","ok"); }
    @GetMapping("/orders") public List<PresentationService.OrderView> orders() { return service.list(); }
    // One local demo process: release this lock only after the service transaction commits.
    @PostMapping("/orders") public synchronized PresentationService.OrderView submit(@RequestBody PresentationService.Submission request,@RequestAttribute(name=AuthAttributes.CUSTOMER_ID,required=false) Long customerId) { return service.submit(request,customerId); }
    public record StatusRequest(String status) {}
    @PutMapping("/orders/{number}/status") public PresentationService.OrderView status(@PathVariable String number,@RequestBody StatusRequest request) { return service.status(number,request.status()); }
}
