package eu.epitech.lil7_games.ui.view;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Array;
import eu.epitech.lil7_games.ui.model.MenuViewModel;

public class SaveManagerPane extends Table {

    private final MenuViewModel viewModel;
    private final List<String> saveList;
    private final Label detailLabel;
    private final TextField saveNameField;
    private TextButton saveButton;

    public SaveManagerPane(Skin skin, MenuViewModel viewModel) {
        super(skin);
        this.viewModel = viewModel;
        defaults().fillX().padTop(10f);

        saveList = new List<>(skin);
        detailLabel = new Label("Select a slot to view details.", skin);
        detailLabel.setWrap(true);
        saveNameField = new TextField("", skin);
        saveNameField.setMessageText("Save name");

        ScrollPane scrollPane = new ScrollPane(saveList, skin);
        add(scrollPane).grow().minHeight(180f);
        row();

        add(detailLabel).growX();
        row();

        add(saveNameField).growX();
        row();

        add(buildActionsTable(skin)).growX();

        View.onChange(saveList, list -> detailLabel.setText(viewModel.describeSlot(list.getSelected())));
        refresh();
        updateSaveButtonState();
    }

    public void setSavingEnabled(boolean enabled) {
        if (saveButton != null) {
            saveButton.setDisabled(!enabled);
        }
        if (saveNameField != null) {
            saveNameField.setDisabled(!enabled);
        }
    }

    public void updateSaveButtonState() {
        if (saveButton != null) {
            saveButton.setDisabled(!viewModel.isGameRunning());
        }
    }

    private Table buildActionsTable(Skin skin) {
        Table actions = new Table();
        actions.defaults().fillX().padTop(8f);

        saveButton = new TextButton("Save", skin);
        saveButton.setDisabled(!viewModel.isGameRunning());
        View.onClick(saveButton, () -> {
            if (viewModel.isGameRunning()) {
                viewModel.saveToSlot(saveNameField.getText());
                saveNameField.setText("");
                refresh();
            }
        });
        actions.add(saveButton).growX();
        actions.row();

        TextButton loadButton = new TextButton("Load", skin);
        View.onClick(loadButton, () -> {
            String selected = saveList.getSelected();
            if (selected != null) {
                viewModel.loadSlot(selected);
                detailLabel.setText(viewModel.describeSlot(selected));
            }
        });
        actions.add(loadButton).growX();
        actions.row();

        TextButton deleteButton = new TextButton("Delete", skin);
        View.onClick(deleteButton, () -> {
            String selected = saveList.getSelected();
            if (selected != null) {
                viewModel.deleteSlot(selected);
                refresh();
            }
        });
        actions.add(deleteButton).growX();

        return actions;
    }

    public void refresh() {
        Array<String> slots = viewModel.getSaveSlots();
        if (slots.size == 0) {
            saveList.setItems();
            saveList.setSelected(null);
            detailLabel.setText("No saves yet. Create one to get started.");
        } else {
            saveList.setItems(copyToArray(slots));
            String latest = viewModel.getLatestSlotName();
            if (latest != null) {
                saveList.setSelected(latest);
            }
            detailLabel.setText(viewModel.describeSlot(saveList.getSelected()));
        }
    }

    private String[] copyToArray(Array<String> source) {
        String[] data = new String[source.size];
        for (int i = 0; i < source.size; i++) {
            data[i] = source.get(i);
        }
        return data;
    }
}
