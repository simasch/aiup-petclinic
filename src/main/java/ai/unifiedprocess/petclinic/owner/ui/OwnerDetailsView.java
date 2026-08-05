package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.owner.domain.Owner;
import ai.unifiedprocess.petclinic.owner.domain.OwnerRepository;
import ai.unifiedprocess.petclinic.ui.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Owner Details view, the navigation target of UC-003: Register New Owner once a new owner is
 * persisted. This shows the owner's own fields; pets, visits, and editing are UC-005/006/007's
 * scope and are not implemented here.
 */
@Route(value = "owners/:" + OwnerRouteParameters.OWNER_ID, layout = MainLayout.class)
@PageTitle("Owner Details")
public class OwnerDetailsView extends VerticalLayout implements BeforeEnterObserver {

    private final OwnerRepository ownerRepository;
    private final H2 name = new H2();
    private final Paragraph address = new Paragraph();
    private final Paragraph city = new Paragraph();
    private final Paragraph telephone = new Paragraph();

    public OwnerDetailsView(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
        add(name, address, city, telephone);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Integer ownerId = event.getRouteParameters().getInteger(OwnerRouteParameters.OWNER_ID)
                .orElseThrow(NotFoundException::new);
        Owner owner = ownerRepository.findById(ownerId).orElseThrow(NotFoundException::new);
        name.setText(owner.firstName() + " " + owner.lastName());
        address.setText(owner.address());
        city.setText(owner.city());
        telephone.setText(owner.telephone());
    }
}
