
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
        .catch(error => toastService.error(error));
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
        .catch(error => toastService.error(error));
}

function updateOus(targetChoice, search) {
    fetch( `/rest/ous/autocomplete?search=${search}`)
        .then(response => response.json()
            .then(data => {
                targetChoice.setChoices(data.content, 'uuid', 'name', true);
            }))
        .catch(error => toastService.error(error));
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
        .catch(error => toastService.error(error));
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
        .catch(error => toastService.error(error));
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

function initOUSelect(elementId, prefetch = true) {
    const ouSelect = document.getElementById(elementId);
    const ouChoices = initSelect(ouSelect);
    if (prefetch) {
        updateOus(ouChoices, "");
    }
    ouSelect.addEventListener("search",
        function(event) {
            updateOus(ouChoices, event.detail.value);
        },
        false,
    );
    return ouChoices;
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
        .catch(error => toastService.error(error));
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
        .catch(error => toastService.error(error));
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
        .catch(error => toastService.error(error));
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
        .catch(error => toastService.error(error));
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
        .catch(error => toastService.error(error));
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
        .catch(error => toastService.error(error));
}

function updateRelationsPrecautionsOnly(choices, search) {
    fetch( `/rest/relatable/autocomplete?types=PRECAUTION&search=${search}`)
        .then(response => response.json()
            .then(data => {
                choices.setChoices(data.content.map(reg => {
                    return {
                        id: reg.id,
                        name: truncateString(reg.typeMessage + ": " + reg.name, 60)
                    }
                }), 'id', 'name', true);
            }))
        .catch(error => toastService.error(error));
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
    Swal.fire({
        text: `Er du sikker på du vil slette relationen?`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#03a9f4',
        cancelButtonColor: '#df5645',
        confirmButtonText: 'Ja',
        cancelButtonText: 'Nej'
    }).then((result) => {
        if (result.isConfirmed) {
            fetch('/relatables/' + id + '/relations/' + relationId + '/' + relationType + '/remove', {
                method: 'DELETE',
                headers: {
                    'X-CSRF-TOKEN': token,
                }
            }).then(() => {
                window.location.reload();
            });
        }
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

function sortColumnById(gridElement, columnId, sortOrder) {
    const header = gridElement.querySelector(`th[data-column-id="${columnId}"]`);

    if (!header) {
        console.error(`No column found with id: ${columnId}`);
        return;
    }

    const sortButton = header.querySelector('.gridjs-sort');
    if (!sortButton) {
        console.error(`No sort button found for column: ${columnId}`);
        return;
    }

    const isNeutral = sortButton.classList.contains('gridjs-sort-neutral');
    const isAscending = sortButton.classList.contains('gridjs-sort-asc');
    const isDescending = sortButton.classList.contains('gridjs-sort-desc');

    if (isNeutral) {
        if (sortOrder === 1) {
            sortButton.click(); // click once for asc

        } else {
            sortButton.click(); // click once for asc
            sortButton.click(); // click once more for desc
        }
    }
    else if (isAscending) {
        if (sortOrder === -1) {
            sortButton.click(); // click once for desc
        }
    }
    else if (isDescending) {
        if (sortOrder === 1) {
            sortButton.click(); // click one for asc
        }
    }
}
