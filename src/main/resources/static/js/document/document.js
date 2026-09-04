import OnUnSubmittedService from "../on-unsubmitted-changes-service.js";
import initRelatedTagList from "../tags/related-tag-list.js";
import { selectOu } from "./ou-select-helper.js";

let onUnSubmittedService = new OnUnSubmittedService();
let userChoicesEditSelect = null;
let ouChoicesEditSelect = null;
let departmentOuChoicesEditSelect = null;

document.addEventListener("DOMContentLoaded", function(event) {
    loadViewAndEditForm();
    addRelationFormLoaded();

    initEditButton();

    const saveEditBtn = document.getElementById("saveEditBtn");
    saveEditBtn?.addEventListener("click", () => {
        onUnSubmittedService.reset();
    });

    initRelatedTagList()
});

function initEditButton() {
    const editButton = document.getElementById("editBtn");
    editButton?.addEventListener("click", function () {
        const responsibleFieldsChangeable = editButton.dataset.responsibleChangeable === 'true';
        editMode(true, responsibleFieldsChangeable);
        onUnSubmittedService.setChangesMade();
    })
}

function formReset() {
    const form = document.querySelector('form');
    form.reset();
}

function loadViewAndEditForm() {
    initDatepicker("#nextRevisionBtn", "#nextRevision");
    userChoicesEditSelect = choiceService.initUserSelect("userSelect");
    ouChoicesEditSelect = choiceService.initOUSelect("ouSelect");
    departmentOuChoicesEditSelect = choiceService.initOUSelect("departmentOuSelect");

    userChoicesEditSelect.passedElement.element.addEventListener('change', function() {
        checkInputField(userChoicesEditSelect);
    });

    userChoicesEditSelect.passedElement.element.addEventListener('addItem', async function() {
        const userUuid = userChoicesEditSelect.passedElement.element.value;
        try {
            const response = await fetch(`/rest/ous/user/${userUuid}/suggestion`);
            if (response.status === 204) {
                ouChoicesEditSelect.removeActiveItems();
                return;
            }
            if (!response.ok) {
                return;
            }
            const suggestedOu = await response.json();
            if (suggestedOu) {
                selectOu(ouChoicesEditSelect, suggestedOu);
            }
        } catch (error) {
            toastService.error(error);
        }
    });

    document.querySelectorAll('.editField').forEach(elem => {
        elem.disabled = true;
    });

    userChoicesEditSelect.disable();
    ouChoicesEditSelect.disable();
    departmentOuChoicesEditSelect.disable();

    initFormValidationForForm("editForm", () => validateChoices(userChoicesEditSelect));
}

function editMode(enabled, responsibleFieldsChangeable = false) {
    if (enabled) {
        document.querySelectorAll('.editField').forEach(elem => {
            elem.disabled = false;
        });

        if (responsibleFieldsChangeable) {
            userChoicesEditSelect.enable();
            ouChoicesEditSelect.enable();
            departmentOuChoicesEditSelect.enable();
        }

        document.getElementById('saveEditBtn').hidden = false;
        document.getElementById('editBtn').hidden = true;
        document.querySelector('.clickableDocLink').style.display = 'none';
        document.querySelector('.editableDocLink').style.display = '';
    } else {
        document.querySelectorAll('.editField').forEach(elem => {
          elem.disabled = true;
        });

        userChoicesEditSelect.disable();
        ouChoicesEditSelect.disable();
        departmentOuChoicesEditSelect.disable();
        document.getElementById('saveEditBtn').hidden = true;
        document.getElementById('editBtn').hidden = false;
        document.querySelector('.clickableDocLink').style.display = '';
        document.querySelector('.editableDocLink').style.display = 'none';
    }
}

function addRelationFormLoaded() {
    choiceService.initDocumentRelationSelect();
}