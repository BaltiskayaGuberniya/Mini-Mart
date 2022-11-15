package com.example.application.views.inventory;

import com.example.application.data.entity.ShopGoods;
import com.example.application.data.service.ShopGoodsService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

@PageTitle("Inventory")
@Route(value = "inventory/:shopGoodsID?/:action?(edit)", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class InventoryView extends Div implements BeforeEnterObserver {

    private final String SHOPGOODS_ID = "shopGoodsID";
    private final String SHOPGOODS_EDIT_ROUTE_TEMPLATE = "inventory/%s/edit";

    private final Grid<ShopGoods> grid = new Grid<>(ShopGoods.class, false);

    private Upload image;
    private Image imagePreview;
    private TextField name;
    private TextField barCode;
    private TextField quantity;
    private TextField pricePerUnit;

    private final Button cancel = new Button("Cancel");
    private final Button save = new Button("Save");

    private final BeanValidationBinder<ShopGoods> binder;

    private ShopGoods shopGoods;

    private final ShopGoodsService shopGoodsService;

    @Autowired
    public InventoryView(ShopGoodsService shopGoodsService) {
        this.shopGoodsService = shopGoodsService;
        addClassNames("inventory-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        LitRenderer<ShopGoods> imageRenderer = LitRenderer.<ShopGoods>of(
                "<span style='border-radius: 50%; overflow: hidden; display: flex; align-items: center; justify-content: center; width: 64px; height: 64px'><img style='max-width: 100%' src=${item.image} /></span>")
                .withProperty("image", item -> {
                    if (item != null && item.getImage() != null) {
                        return "data:image;base64," + Base64.getEncoder().encodeToString(item.getImage());
                    } else {
                        return "";
                    }
                });
        grid.addColumn(imageRenderer).setHeader("Image").setWidth("96px").setFlexGrow(0);

        grid.addColumn("name").setAutoWidth(true);
        grid.addColumn("barCode").setAutoWidth(true);
        grid.addColumn("quantity").setAutoWidth(true);
        grid.addColumn("pricePerUnit").setAutoWidth(true);
        grid.setItems(query -> shopGoodsService.list(
                PageRequest.of(query.getPage(), query.getPageSize(), VaadinSpringDataHelpers.toSpringDataSort(query)))
                .stream());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(SHOPGOODS_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(InventoryView.class);
            }
        });

        // Configure Form
        binder = new BeanValidationBinder<>(ShopGoods.class);

        // Bind fields. This is where you'd define e.g. validation rules
        binder.forField(quantity).withConverter(new StringToIntegerConverter("Only numbers are allowed"))
                .bind("quantity");
        binder.forField(pricePerUnit).withConverter(new StringToIntegerConverter("Only numbers are allowed"))
                .bind("pricePerUnit");

        binder.bindInstanceFields(this);

        attachImageUpload(image, imagePreview);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.shopGoods == null) {
                    this.shopGoods = new ShopGoods();
                }
                binder.writeBean(this.shopGoods);
                shopGoodsService.update(this.shopGoods);
                clearForm();
                refreshGrid();
                Notification.show("ShopGoods details stored.");
                UI.getCurrent().navigate(InventoryView.class);
            } catch (ValidationException validationException) {
                Notification.show("An exception happened while trying to store the shopGoods details.");
            }
        });

    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<UUID> shopGoodsId = event.getRouteParameters().get(SHOPGOODS_ID).map(UUID::fromString);
        if (shopGoodsId.isPresent()) {
            Optional<ShopGoods> shopGoodsFromBackend = shopGoodsService.get(shopGoodsId.get());
            if (shopGoodsFromBackend.isPresent()) {
                populateForm(shopGoodsFromBackend.get());
            } else {
                Notification.show(String.format("The requested shopGoods was not found, ID = %s", shopGoodsId.get()),
                        3000, Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(InventoryView.class);
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
        Label imageLabel = new Label("Image");
        imagePreview = new Image();
        imagePreview.setWidth("100%");
        image = new Upload();
        image.getStyle().set("box-sizing", "border-box");
        image.getElement().appendChild(imagePreview.getElement());
        name = new TextField("Name");
        barCode = new TextField("Bar Code");
        quantity = new TextField("Quantity");
        pricePerUnit = new TextField("Price Per Unit");
        formLayout.add(imageLabel, image, name, barCode, quantity, pricePerUnit);

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

    private void attachImageUpload(Upload upload, Image preview) {
        ByteArrayOutputStream uploadBuffer = new ByteArrayOutputStream();
        upload.setAcceptedFileTypes("image/*");
        upload.setReceiver((fileName, mimeType) -> {
            uploadBuffer.reset();
            return uploadBuffer;
        });
        upload.addSucceededListener(e -> {
            StreamResource resource = new StreamResource(e.getFileName(),
                    () -> new ByteArrayInputStream(uploadBuffer.toByteArray()));
            preview.setSrc(resource);
            preview.setVisible(true);
            if (this.shopGoods == null) {
                this.shopGoods = new ShopGoods();
            }
            this.shopGoods.setImage(uploadBuffer.toByteArray());
        });
        preview.setVisible(false);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(ShopGoods value) {
        this.shopGoods = value;
        binder.readBean(this.shopGoods);
        this.imagePreview.setVisible(value != null);
        if (value == null || value.getImage() == null) {
            this.image.clearFileList();
            this.imagePreview.setSrc("");
        } else {
            this.imagePreview.setSrc("data:image;base64," + Base64.getEncoder().encodeToString(value.getImage()));
        }

    }
}
