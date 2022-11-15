package com.example.application.views.costumers;

import com.example.application.data.entity.Costumers;
import com.example.application.data.service.CostumersService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

@PageTitle("Costumers")
@Route(value = "Costumers/:costumersID?/:action?(edit)", layout = MainLayout.class)
@RolesAllowed("ADMIN")
@Uses(Icon.class)
public class CostumersView extends Div implements BeforeEnterObserver {

    private final String COSTUMERS_ID = "costumersID";
    private final String COSTUMERS_EDIT_ROUTE_TEMPLATE = "Costumers/%s/edit";

    private final Grid<Costumers> grid = new Grid<>(Costumers.class, false);

    private TextField name;
    private TextField email;
    private TextField phone;
    private DatePicker lastPurchase;
    private Checkbox hasMembershipCard;

    private final Button cancel = new Button("Cancel");
    private final Button save = new Button("Save");

    private final BeanValidationBinder<Costumers> binder;

    private Costumers costumers;

    private final CostumersService costumersService;

    @Autowired
    public CostumersView(CostumersService costumersService) {
        this.costumersService = costumersService;
        addClassNames("costumers-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        grid.addColumn("name").setAutoWidth(true);
        grid.addColumn("email").setAutoWidth(true);
        grid.addColumn("phone").setAutoWidth(true);
        grid.addColumn("lastPurchase").setAutoWidth(true);
        LitRenderer<Costumers> hasMembershipCardRenderer = LitRenderer.<Costumers>of(
                "<vaadin-icon icon='vaadin:${item.icon}' style='width: var(--lumo-icon-size-s); height: var(--lumo-icon-size-s); color: ${item.color};'></vaadin-icon>")
                .withProperty("icon", hasMembershipCard -> hasMembershipCard.isHasMembershipCard() ? "check" : "minus")
                .withProperty("color",
                        hasMembershipCard -> hasMembershipCard.isHasMembershipCard()
                                ? "var(--lumo-primary-text-color)"
                                : "var(--lumo-disabled-text-color)");

        grid.addColumn(hasMembershipCardRenderer).setHeader("Has Membership Card").setAutoWidth(true);

        grid.setItems(query -> costumersService.list(
                PageRequest.of(query.getPage(), query.getPageSize(), VaadinSpringDataHelpers.toSpringDataSort(query)))
                .stream());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(COSTUMERS_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(CostumersView.class);
            }
        });

        // Configure Form
        binder = new BeanValidationBinder<>(Costumers.class);

        // Bind fields. This is where you'd define e.g. validation rules

        binder.bindInstanceFields(this);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.costumers == null) {
                    this.costumers = new Costumers();
                }
                binder.writeBean(this.costumers);
                costumersService.update(this.costumers);
                clearForm();
                refreshGrid();
                Notification.show("Costumers details stored.");
                UI.getCurrent().navigate(CostumersView.class);
            } catch (ValidationException validationException) {
                Notification.show("An exception happened while trying to store the costumers details.");
            }
        });

    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<UUID> costumersId = event.getRouteParameters().get(COSTUMERS_ID).map(UUID::fromString);
        if (costumersId.isPresent()) {
            Optional<Costumers> costumersFromBackend = costumersService.get(costumersId.get());
            if (costumersFromBackend.isPresent()) {
                populateForm(costumersFromBackend.get());
            } else {
                Notification.show(String.format("The requested costumers was not found, ID = %s", costumersId.get()),
                        3000, Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(CostumersView.class);
            }
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        name = new TextField("Name");
        email = new TextField("Email");
        phone = new TextField("Phone");
        lastPurchase = new DatePicker("Last Purchase");
        hasMembershipCard = new Checkbox("Has Membership Card");
        formLayout.add(name, email, phone, lastPurchase, hasMembershipCard);

        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, cancel);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        splitLayout.addToPrimary(wrapper);
        wrapper.add(grid);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(Costumers value) {
        this.costumers = value;
        binder.readBean(this.costumers);

    }
}
