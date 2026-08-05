package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.owner.domain.Owner;
import ai.unifiedprocess.petclinic.owner.domain.OwnerRepository;
import ai.unifiedprocess.petclinic.ui.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC-003: Register New Owner.
 */
@Route(value = "owners/new", layout = MainLayout.class)
@PageTitle("Add Owner")
public class AddOwnerView extends VerticalLayout {

    public AddOwnerView(OwnerRepository ownerRepository) {
        OwnerForm ownerForm = new OwnerForm();
        Button addOwnerButton = new Button("Add Owner");
        addOwnerButton.addClickListener(event -> ownerForm.validateAndRead(null).ifPresentOrElse(
                owner -> {
                    Owner saved = ownerRepository.insert(owner);
                    Notification.show("New Owner Created");
                    UI.getCurrent().navigate(OwnerDetailsView.class, OwnerRouteParameters.forOwner(saved.id()));
                },
                () -> Notification.show("There was an error in creating the owner.")));

        add(new H2("New Owner"), ownerForm, addOwnerButton);
    }
}
