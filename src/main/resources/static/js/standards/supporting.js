const editorDescriptionInstances = {};
const editorPracticeInstances = {};
const editorSmartInstances = {};
function supportingStandartsViewLoaded() {
    const userSelects = document.querySelectorAll('.responsibleUserSelect');
    userSelects.forEach(select => {
        var choiceSelect = choiceService.initUserSelect(select.id, false);
        choiceSelect.passedElement.element.addEventListener('addItem', function() {
            var selectElement = choiceSelect.passedElement.element;
            var selectedIndex = selectElement.selectedIndex;
            var selectedOptionText = selectElement.options[selectedIndex].text;
            var username = findTextInParentheses(selectedOptionText)[0];
            var id = select.dataset.id;
            var index = select.dataset.index;

            setField(id, "RESPONSIBLE", choiceSelect.passedElement.element.value, index)
            document.getElementById('responsibleTD' + index).textContent = username;
        });

        choiceSelect.passedElement.element.addEventListener('removeItem', function() {
            if(!choiceSelect.getValue()) {
                var id = select.dataset.id;
                var index = select.dataset.index;
                document.getElementById('responsibleTD' + index).textContent = "";
                setField(id, "RESPONSIBLE", "", index);
            }
        });
    });

    const foldableRows = document.querySelectorAll('.foldable');
    foldableRows.forEach(row => {
        row.addEventListener('click', () => {
            const nextRow = row.nextElementSibling;
            let index = row.dataset.index;

            if (nextRow.classList.contains('hidden')) {
                nextRow.classList.remove('hidden');
            } else {
                nextRow.classList.add('hidden');

                if (editorDescriptionInstances[index] &&
                    document.getElementById('editorDescriptionContainer' + index) != null &&
                    !document.getElementById('editorDescriptionContainer' + index).hidden) {

                    var editorElement = editorDescriptionInstances[index].editor;
                    document.getElementById('descriptionRead' + index).innerHTML = editorElement.getData();
                    document.getElementById('editorDescriptionContainer' + index).hidden = true;
                    document.getElementById('descriptionRead' + index).hidden = false;
                    editorDescriptionInstances[index].editing = false;
                }
                if (editorPracticeInstances[index] &&
                    document.getElementById('editorPracticeContainer' + index) != null &&
                    !document.getElementById('editorPracticeContainer' + index).hidden) {

                    var editorElement = editorPracticeInstances[index].editor;
                    document.getElementById('practiceRead' + index).innerHTML = editorElement.getData();
                    document.getElementById('editorPracticeContainer' + index).hidden = true;
                    document.getElementById('practiceRead' + index).hidden = false;
                    editorPracticeInstances[index].editing = false;
                }
                if (editorSmartInstances[index] &&
                    document.getElementById('editorSmartContainer' + index) != null &&
                    !document.getElementById('editorSmartContainer' + index).hidden) {

                    var editorElement = editorSmartInstances[index].editor;
                    document.getElementById('smartRead' + index).innerHTML = editorElement.getData();
                    document.getElementById('editorSmartContainer' + index).hidden = true;
                    document.getElementById('smartRead' + index).hidden = false;
                    editorSmartInstances[index].editing = false;
                }
            }
        });
    });

    const statusSelects = document.querySelectorAll('.statusSelect');
    statusSelects.forEach(select => {
        select.addEventListener('change', function() {
            var id = select.dataset.id;
            var index = select.dataset.index;
            var value = select.value;

            setField(id, "STATUS", value, index)

            if (value === "READY") {
                document.getElementById('statusTD' + index).innerHTML = '<div class="d-block badge bg-green-500" style="width: 60px">Klar</div>';
            } else if (value === "IN_PROGRESS") {
                document.getElementById('statusTD' + index).innerHTML = '<div class="d-block badge bg-blue-500" style="width: 60px">I gang</div>';
            } else if (value === "NOT_STARTED") {
                document.getElementById('statusTD' + index).innerHTML = '<div class="d-block badge bg-yellow-500" style="width: 60px">Ikke startet</div>';
            } else if (value === "NOT_RELEVANT") {
                document.getElementById('statusTD' + index).innerHTML = '<div class="d-block badge bg-gray-500" style="width: 60px">Ikke relevant</div>';
            }
        });
    });

    const reasons = document.querySelectorAll('.reasons');
    reasons.forEach(input => {
        input.addEventListener('change', function() {
            var id = input.dataset.id;
            var index = input.dataset.index;
            var value = input.value;

            setField(id, "REASON", value, index)
            document.getElementById('reasonTD' + index).textContent = value
        });
    });

    const addRelationBtns = document.querySelectorAll('.addRelationBtn');
    addRelationBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            document.getElementById('relatableIdInput').value = btn.dataset.relatableid;
            document.getElementById('standardSectionStatIndex').value = btn.dataset.index;
        });
    });

    document.getElementById('customAddRelationBtn').addEventListener('click', addRelations);

    const selectCheckboxes = document.querySelectorAll('.selectedCheckbox');
    selectCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            var id = checkbox.dataset.id;
            var index = checkbox.dataset.index;
            var checked = checkbox.checked;
            setField(id, "SELECTED", checked, index);

            if (checked) {
                document.getElementById('statusTD' + index).style.display = "block";
                document.getElementById('selectedTD' + index).textContent = "Tilvalgt";
            } else {
                document.getElementById('statusTD' + index).style.display = "none";
                document.getElementById('selectedTD' + index).textContent = "Fravalgt";
            }
        });
    });
}

function editField(elem) {
    let index = elem.dataset.index;
    let type = elem.dataset.type;

    if (type === 'DESCRIPTION' && editorDescriptionInstances[index]) {
        showEditor(index, type);
        return;
    }
    if (type === 'NSIS_PRACTICE' && editorPracticeInstances[index]) {
        showEditor(index, type);
        return;
    }
    if (type === 'NSIS_SMART' && editorSmartInstances[index]) {
        showEditor(index, type);
        return;
    }

    // first-time initialization
    const editorContainer = document.getElementById(
        type === 'DESCRIPTION' ? 'editorDescriptionContainer' + index :
            type === 'NSIS_PRACTICE' ? 'editorPracticeContainer' + index :
                'editorSmartContainer' + index
    );

    const elemToEdit = editorContainer.querySelector('.descriptions');

    window.CreateCkEditor(elemToEdit, editor => {
        const id = editor.sourceElement.dataset.id;
        const editorObj = {editor, editing: true};

        // save instance
        if (type === 'DESCRIPTION') editorDescriptionInstances[index] = editorObj;
        if (type === 'NSIS_PRACTICE') editorPracticeInstances[index] = editorObj;
        if (type === 'NSIS_SMART') editorSmartInstances[index] = editorObj;

        // handle blur → save field
        editor.editing.view.document.on('blur', () => {
            setField(id, type, editor.getData(), index);
        });

        showEditor(index, type);
    });
}

function showEditor(index, type) {
    if (type === 'DESCRIPTION') {
        document.getElementById('descriptionRead' + index).hidden = true;
        document.getElementById('editorDescriptionContainer' + index).hidden = false;
        editorDescriptionInstances[index].editing = true;
    } else if (type === 'NSIS_PRACTICE') {
        document.getElementById('practiceRead' + index).hidden = true;
        document.getElementById('editorPracticeContainer' + index).hidden = false;
        editorPracticeInstances[index].editing = true;
    } else if (type === 'NSIS_SMART') {
        document.getElementById('smartRead' + index).hidden = true;
        document.getElementById('editorSmartContainer' + index).hidden = false;
        editorSmartInstances[index].editing = true;
    }
}

function setField(standardSectionId, setFieldType, value, index) {
    var data = {
        "setFieldType": setFieldType,
        "value": value
    };

    postData("/rest/standards/" + templateIdentifier + "/supporting/standardsection/" + standardSectionId, data).then((response) => {
        if (!response.ok) {
            throw new Error(`${response.status} ${response.statusText}`);
        }
    }).catch(function(error) {
        toastService.error(error);
        console.error(error);
        window.location.reload();
    });
    toastService.info("Opdatering gemt!");
    // set last updated date
    document.getElementById('dateTD' + index).textContent = today;
}

function findTextInParentheses(str) {
    // Regular expression for at finde tekst mellem parenteser
    var regex = /\(([^)]+)\)/g;
    // Brug match() metoden til at finde tekst mellem parenteser
    var matches = str.match(regex);
    // Tjek om der er nogen matches, og returner dem
    if (matches) {
        return matches.map(match => match.slice(1, -1)); // Fjern parenteserne
    } else {
        return []; // Returner en tom array hvis der ikke er nogen matches
    }
}

var relationsChoice;
function addRelationFormLoaded() {
    const relationsSelect = document.getElementById('relationsSelect');
    relationsChoice = initSelect(relationsSelect);
    choiceService.updateRelationsForStandardSection(relationsChoice, "");
    relationsSelect.addEventListener("search",
        function(event) {
            choiceService.updateRelationsForStandardSection(relationsChoice, event.detail.value);
        },
        false,
    );
    relationsSelect.addEventListener("change",
        function(event) {
            choiceService.updateRelationsForStandardSection(relationsChoice, "");
        },
        false,
    );
}

function addRelations() {
    var relatableId = document.getElementById('relatableIdInput').value;
    var data = {
        "relatableId": relatableId,
        "relations": relationsChoice.getValue(true)
    };

    postData("/rest/relatable/relations/add", data).then((response) => {
        if (!response.ok) {
            throw new Error('Network response was not ok: ' + response.status);
        }
        return response.json();
    }).then(function(responseData) {
        var modalElement = document.getElementById('addRelationModal');
        var modal = bootstrap.Modal.getInstance(modalElement);
        modal.hide();
        relationsChoice.removeActiveItems();

        var index = document.getElementById('standardSectionStatIndex').value;
        var table = document.getElementById("relationTable" + index);
        responseData.forEach(function(item) {
            var row = table.insertRow();
            var cell1 = row.insertCell(0);
            var cell2 = row.insertCell(1);
            var cell3 = row.insertCell(2);
            cell1.innerHTML = '<a href="/' + item.typeForUrl + '/' + (item.standardIdentifier == null ? item.id : item.standardIdentifier) + '"><span>' + item.title + '</span></a>';
            cell2.innerHTML = item.typeMessage;
            cell3.innerHTML = '<i class="pli-cross fs-5 me-2" onclick="customDeleteRelation(this)" data-relatableid="' + relatableId + '" data-relationid="' + item.id + '" data-relationtype="' + item.type + '"></i>';
        });
        toastService.info("Relation tilknyttet");
        return responseData; // Dette returneres som et promise-svar
    })
        .catch(function(error) {
            toastService.error(error);
            console.error(error);
            window.location.reload();
        });
}

function customDeleteRelation(element) {
    var id = element.dataset.relatableid;
    var relationId = element.dataset.relationid;
    var relationType = element.dataset.relationtype;
    var token = document.getElementsByName("_csrf")[0].getAttribute("content");

    fetch('/relatables/' + id + '/relations/' + relationId + '/' + relationType + '/remove', {
        method: 'DELETE',
        headers: {
            'X-CSRF-TOKEN': token,
        }
    }).then((response) => {
        var trElement = element.closest('tr');
        if (trElement) {
            var tbody = trElement.closest('tbody');
            tbody.removeChild(trElement);
        } else {
            window.location.reload();
        }
    });
}

function filterOnStatusChanged() {
    var selectedStatus = document.getElementById('statusFilter').value;
    if (selectedStatus == "ALL") {
        window.location.href = viewUrl + templateIdentifier;
    } else {
        window.location.href = viewUrl + templateIdentifier + "?status=" + selectedStatus;
    }
}