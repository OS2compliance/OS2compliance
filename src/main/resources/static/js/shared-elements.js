
function updateDocumentRelations(choices, search) {
    fetch( `/rest/relatable/autocomplete?types=DOCUMENT&search=${search}`)
        .then(response => response.json()
            .then(data => {
                choices.setChoices(data.content.map(reg => {
                    return {
                        id: reg.id,
                        name: truncateString(reg.name, 60)
                    }
                }), 'id', 'name', true);
            }))
        .catch(error => console.log(error));
}

function updateUsers(targetChoice, search) {
    fetch( `/rest/users/autocomplete?search=${search}`)
        .then(response => response.json()
            .then(data => {
                targetChoice.setChoices(data.content.map(e => {
                    return {
                        value: e.uuid,
                        label: `(${e.userId}) ${e.name}`}
                }), 'value', 'label', true);
            }))
        .catch(error => console.log(error));
}
function updateKitos(targetChoice, search) {
    fetch( `/rest/kitos/autocomplete?search=${search}`)
        .then(response => response.json()
            .then(data => {
                targetChoice.setChoices(data.content.map(e => {
                    return {
                        value: e.uuid,
                        label: `(${e.name}) ${e.uuid}`}
                }), 'value', 'label', true);
            }))
        .catch(error => console.log(error));
}


function updateOus(targetChoice, search) {
    fetch( `/rest/ous/autocomplete?search=${search}`)
        .then(response => response.json()
            .then(data => {
                targetChoice.setChoices(data.content, 'uuid', 'name', true);
            }))
        .catch(error => console.log(error));
}

function updateRelations(choices, search) {
    fetch( `/rest/relatable/autocomplete?search=${search}`)
        .then(response => response.json()
            .then(data => {
                choices.setChoices(data.content.map(reg => {
                    return {
                        id: reg.id + "",
                        name: truncateString(reg.typeMessage + ": " + reg.name, 60)
                    }
                }), 'id', 'name', true);
            }))
        .catch(error => console.log(error));
}

function updateTags(choices, search) {
    fetch( `/rest/relatable/tags/autocomplete?search=${search}`)
        .then(response => response.json()
            .then(data => {
                choices.setChoices(data.content.map(reg => {
                    return {
                        id: reg.id + "",
                        name: reg.value
                    }
                }), 'id', 'name', true);
            }))
        .catch(error => console.log(error));
}

function initUserSelect(elementId, prefetch = true) {
    const userSelect = document.getElementById(elementId);
    const userChoices = initSelect(userSelect);
    if (prefetch) {
        updateUsers(userChoices, "");
    }
    userSelect.addEventListener("search",
        function(event) {
            updateUsers(userChoices, event.detail.value);
        },
        false,
    );
    return userChoices;
}

function initKitosSelect(elementId) {
    const select = document.getElementById(elementId);
    if (select === null) {
        return;
    }
    const choices = initSelect(select);
    updateKitos(choices, "");
    select.addEventListener("search",
        function (event) {
            updateKitos(choices, event.detail.value)
        },
        false
    );
    return choices;

}

function initOUSelect(elementId) {
    const ouSelect = document.getElementById(elementId);
    const ouChoices = initSelect(ouSelect);
    updateOus(ouChoices, "");
    ouSelect.addEventListener("search",
        function(event) {
            updateOus(ouChoices, event.detail.value);
        },
        false,
    );
    return ouChoices;
}

let createTaskRelationSelect = null;
function initCreateTaskRelationSelect() {
    const relationsSelect = document.getElementById('createTaskRelationsSelect');
    const relationsChoice = initSelect(relationsSelect);
    updateRelations(relationsChoice, "");
    relationsSelect.addEventListener("search",
        function(event) {
            updateRelations(relationsChoice, event.detail.value);
        },
        false,
    );
    relationsSelect.addEventListener("change",
        function(event) {
            updateRelations(relationsChoice, "");
        },
        false,
    );
    createTaskRelationSelect = relationsChoice;
}

function initTagSelect(id) {
    const tagsSelect = document.getElementById(id);
    const tagsChoice = initSelect(tagsSelect);
    updateTags(tagsChoice, "");
    tagsSelect.addEventListener("search",
        function(event) {
            updateTags(tagsChoice, event.detail.value);
        },
        false,
    );
    tagsSelect.addEventListener("change",
        function(event) {
            updateTags(tagsChoice, "");
        },
        false,
    );
    createTaskTagSelect = tagsChoice;
}

function initDocumentRelationSelect() {
    const relationsSelect = document.getElementById('relationsSelect');
    const relationsChoice = initSelect(relationsSelect);
    updateRelationsForDocument(relationsChoice, "");
    relationsSelect.addEventListener("search",
        function(event) {
            updateRelationsForDocument(relationsChoice, event.detail.value);
        },
        false,
    );
    relationsSelect.addEventListener("change",
        function(event) {
            updateRelationsForDocument(relationsChoice, "");
        },
        false,
    );
}

function updateRelationsForDocument(choices, search) {
    fetch( `/rest/relatable/autocomplete?types=TASK,SUPPLIER,ASSET,REGISTER,STANDARD_SECTION&search=${search}`)
        .then(response => response.json()
            .then(data => {
                choices.setChoices(data.content.map(reg => {
                    return {
                        id: reg.id,
                        name: truncateString(reg.typeMessage + ": " + reg.name, 60)
                    }
                }), 'id', 'name', true);
            }))
        .catch(error => console.log(error));
}

function updateRelationsAssetsOnly(choices, search) {
    fetch( `/rest/relatable/autocomplete?types=ASSET&search=${search}`)
        .then(response => response.json()
            .then(data => {
                choices.setChoices(data.content.map(reg => {
                    return {
                        id: reg.id,
                        name: truncateString(reg.typeMessage + ": " + reg.name, 60)
                    }
                }), 'id', 'name', true);
            }))
        .catch(error => console.log(error));
}

function updateRelationsTasksOnly(choices, search) {
    fetch( `/rest/relatable/autocomplete?types=TASK&search=${search}`)
        .then(response => response.json()
            .then(data => {
                choices.setChoices(data.content.map(reg => {
                    return {
                        id: reg.id,
                        name: truncateString(reg.typeMessage + ": " + reg.name, 60)
                    }
                }), 'id', 'name', true);
            }))
        .catch(error => console.log(error));
}

function updateRelationsDocumentsOnly(choices, search) {
    fetch( `/rest/relatable/autocomplete?types=DOCUMENT&search=${search}`)
        .then(response => response.json()
            .then(data => {
                choices.setChoices(data.content.map(reg => {
                    return {
                        id: reg.id,
                        name: truncateString(reg.typeMessage + ": " + reg.name, 60)
                    }
                }), 'id', 'name', true);
            }))
        .catch(error => console.log(error));
}

function updateRelationsRegistersOnly(choices, search) {
    fetch( `/rest/relatable/autocomplete?types=REGISTER&search=${search}`)
        .then(response => response.json()
            .then(data => {
                choices.setChoices(data.content.map(reg => {
                    return {
                        id: reg.id,
                        name: truncateString(reg.typeMessage + ": " + reg.name, 60)
                    }
                }), 'id', 'name', true);
            }))
        .catch(error => console.log(error));
}

function updateRelationsForStandardSection(choices, search) {
    fetch( `/rest/relatable/autocomplete?types=TASK,DOCUMENT,STANDARD_SECTION&search=${search}`)
        .then(response => response.json()
            .then(data => {
                choices.setChoices(data.content.map(reg => {
                    return {
                        id: reg.id,
                        name: truncateString(reg.typeMessage + ": " + reg.name, 60)
                    }
                }), 'id', 'name', true);
            }))
        .catch(error => console.log(error));
}

function selectCreateTaskOption(value) {
    const form = document.querySelector('#taskCreateForm');
    const repetitionField = form.querySelector('#repetition');
    if (value === 'TASK') {
        repetitionField.value = 'NONE';
    }
    repetitionField.disabled = value !== 'CHECK';
}

function taskFormReset() {
    const form = document.querySelector('#taskCreateForm');
    form.reset();
    selectCreateTaskOption('TASK');
}

let createTaskUserChoicesEditSelect = null;
let createTaskOuChoicesEditSelect = null;

function sharedTaskFormLoaded() {
    selectCreateTaskOption('TASK');

    initDatepicker("#taskDeadlineBtn", "#taskDeadline");
    createTaskUserChoicesEditSelect = initUserSelect('taskUserSelect');
    createTaskOuChoicesEditSelect = initOUSelect('taskOuSelect');
    createTaskUserChoicesEditSelect.passedElement.element.addEventListener('change', function() {
        checkInputField(createTaskUserChoicesEditSelect);
    });
    initFormValidationForForm('taskCreateForm',
        () => validateChoices(createTaskUserChoicesEditSelect, createTaskOuChoicesEditSelect));
}

function loadSettingElement(pageType){
    return fetch(`/settings/form`)
    .then(response => response.text()
        .then(data => {
            document.getElementById('settings').innerHTML = data;;
        }))
    .catch(error => console.log(error));
}


function createDocumentFormLoaded() {
    initDatepicker("#nextRevisionBtn", "#nextRevision");
    userChoicesEditSelect = initUserSelect('userSelect');
    initDocumentRelationSelect();
    initTagSelect('createDocumentTagsSelect');

    userChoicesEditSelect.passedElement.element.addEventListener('change', function() {
        checkInputField(userChoicesEditSelect);
    });
    initFormValidationForForm("createDocumentModal", () => validateChoices(userChoicesEditSelect));
}

function deleteRelation(element) {
    var id = element.dataset.relatableid;
    var relationId = element.dataset.relationid;
    var relationType = element.dataset.relationtype;
    var token = document.getElementsByName("_csrf")[0].getAttribute("content");

    fetch('/relatables/' + id + '/relations/' + relationId + '/' + relationType + '/remove', {
        method: 'DELETE',
        headers: {
            'X-CSRF-TOKEN': token,
        }
    }).then(() => {
        window.location.reload();
    });
}

function addTagsFormLoaded() {
    initTagSelect("tagsSelect");
}

function deleteTag(element) {
    var id = element.dataset.relatableid;
    var tagId = element.dataset.tagid;
    var token = document.getElementsByName("_csrf")[0].getAttribute("content");

    fetch('/relatables/' + id + '/tags/' + tagId + '/remove', {
        method: 'DELETE',
        headers: {
            'X-CSRF-TOKEN': token,
        }
    }).then(() => {
        window.location.reload();
    });
}
