package ai.unifiedprocess.petclinic.vet.web;

import ai.unifiedprocess.petclinic.vet.domain.VetRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC-002 alternative flow A1: Request Vets as JSON/XML.
 */
@RestController
public class VetsController {

    private final VetRepository vetRepository;

    public VetsController(VetRepository vetRepository) {
        this.vetRepository = vetRepository;
    }

    @GetMapping(value = "/vets", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Vets vets() {
        return new Vets(vetRepository.findAll());
    }
}
