
    const defaultClassName = {
        table: 'table table-striped',
        search: "form-control",
        header: "d-flex justify-content-end"
    };

    document.addEventListener("DOMContentLoaded", function(event) {
        const defaultClassName = {
            table: 'table table-striped',
            search: "form-control",
            header: "d-flex justify-content-end"
        };

        let gridConfig = {
            className: defaultClassName,
            columns: [
                {
                    name: "id",
                    hidden: true
                },
                {
                    name: "Risikovurdering",
                    searchable: {
                        searchKey: 'name'
                    },
                    formatter: (cell, row) => {
                        const external = row.cells[10]['data']
                        const externalLink = row.cells[11]['data']
                        const url = viewUrl + row.cells[0]['data'];
                        if(external) {
                            return gridjs.html(`<a href="${externalLink}" target="_blank">${cell} (Ekstern)</a>`);
                        } else {
                            return gridjs.html(`<a href="${url}">${cell}</a>`);
                        }
                    }
                },
                {
                    name: "Type",
                    searchable: {
                        searchKey: 'type',
                        fieldId :'riskThreatAssessmentSearchSelector'
                    },
                    width: '120px',
                },
                {
                    name: "Fagområde",
                    searchable: {
                        searchKey: 'responsibleOU.name'
                    },
                },
                {
                    name: "Risikoejer",
                    searchable: {
                        searchKey: 'responsibleUser.name'
                    },
                },
                {
                    name: "Opgaver",
                    searchable: {
                        sortKey: 'tasks'
                    },
                    width: '120px',
                },
                {
                    name: "Dato",
                    searchable: {
                        searchKey: 'date'
                    },
                    width: '120px',
                },
                {
                    name: "Status",
                    width: '120px',
                    searchable: {
                        searchKey: 'threatAssessmentReportApprovalStatus',
                        fieldId: 'riskStatusSearchSelector'
                    },
                },
                {
                    name: "Risikovurdering",
                    searchable: {
                        searchKey: 'assessment',
                       fieldId: 'riskAssessmentSearchSelector'
                    },
                    width: '120px',
                    formatter: (cell, row) => {
                        var status = cell;
                        if (cell === "Grøn") {
                            status = [
                                '<div class="d-block badge bg-green">' + cell + '</div>'
                            ]
                        } else if (cell === "Lysgrøn") {
                            status = [
                                '<div class="d-block badge bg-green-300">' + cell + '</div>'
                            ]
                        } else if (cell === "Gul") {
                            status = [
                                '<div class="d-block badge bg-yellow">' + cell + '</div>'
                            ]
                        } else if (cell === "Orange") {
                            status = [
                                '<div class="d-block badge bg-orange">' + cell + '</div>'
                            ]
                        } else if (cell === "Rød") {
                            status = [
                                '<div class="d-block badge bg-danger">' + cell + '</div>'
                            ]
                        } else if (cell === "NONE") {
                            return "";
                        }
                        return gridjs.html(''.concat(...status), 'div')
                    },
                },
                {
                    id: 'handlinger',
                    name: 'Handlinger',
                    sort: 0,
                    width: '100px',
                    formatter: (cell, row) => {
                        const riskId = row.cells[0]['data'];
                        const name = row.cells[1]['data'].replaceAll("'", "\\'");
                        const external = row.cells[10]['data']
                        const externalLink = row.cells[11]['data']
                        const changeable = row.cells[9]['data']
                        let buttonHTML = ''

                        //edit button
                        if ((superuser || changeable)
                            && external) {
                            buttonHTML = buttonHTML + `<button type="button" class="btn btn-icon btn-outline-light btn-xs ms-1" onclick="createExternalRiskassessmentService.editExternalClicked('${riskId}')"><i class="pli-pencil fs-5"></i></button>`
                        } else if(superuser || changeable) {
                            buttonHTML = buttonHTML +
                                `<button type="button" class="btn btn-icon btn-outline-light btn-xs" onclick="editRiskService.showEditDialog('${riskId}')"><i class="pli-pencil fs-5"></i></button>`
                                +`<button type="button" class="btn btn-icon btn-outline-light btn-xs ms-1" onclick="copyRiskService.showCopyDialog('${riskId}')"><i class="pli-data-copy fs-5"></i></button>`
                        }
                        //delete & copy buttons
                        if (superuser) {
                            buttonHTML = buttonHTML +
                                `<button type="button" class="btn btn-icon btn-outline-light btn-xs ms-1" onclick="deleteClicked('${riskId}', '${name.replaceAll('\"', '')}')"><i class="pli-trash fs-5"></i></button>`
                        }
                        return  gridjs.html(buttonHTML)
                    }
                },
                {
                    name: "fromExternalSource",
                    hidden: true
                },
                {
                    name: "externalLink",
                    hidden: true
                },
            ],
            server:{
                url: gridRisksUrl,
                method: 'POST',
                headers: {
                    'X-CSRF-TOKEN': token
                },
                then: data => data.content.map(risk =>
                    [ risk.id, risk.name, risk.type, risk.responsibleOU, risk.responsibleUser, risk.tasks, risk.date, risk.threatAssessmentReportApprovalStatus, risk.assessment, risk.changeable]
                ),
                then: data => data.content.map(obj => {
                        const result = []
                        for (const property of columnProperties) {
                            result.push(obj[property])
                        }
                        return result;
                    }
                ),
                total: data => data.totalCount
            },
            language: {
                'search': {
                    'placeholder': 'Søg'
                },
                'pagination': {
                    'previous': 'Forrige',
                    'next': 'Næste',
                    'showing': 'Viser',
                    'results': 'Opgaver',
                    'of': 'af',
                    'to': 'til',
                    'navigate': (page, pages) => `Side ${page} af ${pages}`,
                    'page': (page) => `Side ${page}`
                }
            }
        };
        const grid = new gridjs.Grid(gridConfig).render( document.getElementById( "risksDatatable" ));

        new CustomGridFunctions(grid, gridRisksUrl, 'risksDatatable')

        gridOptions.init(grid, document.getElementById("gridOptions"));
    });

function deleteClicked(riskId, name) {
    Swal.fire({
      text: `Er du sikker på du vil slette "${name}"?\nReferencer til og fra risikovurderingen slettes også.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#03a9f4',
      cancelButtonColor: '#df5645',
      confirmButtonText: 'Ja',
      cancelButtonText: 'Nej'
    }).then((result) => {
      if (result.isConfirmed) {
        fetch(`${deleteUrl}${riskId}`, { method: 'DELETE', headers: { 'X-CSRF-TOKEN': token} })
                .then(() => {
                    window.location.reload();
                });
      }
    })
}

function formReset() {
    const form = document.querySelector('form');
    form.reset();
}

function updateTypeSelect(choices, search, types) {
    fetch( `/rest/relatable/autocomplete?types=${types}&search=${search}`)
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

function initRegisterSelect(registerSelectElement) {
    const registerChoices = initSelect(registerSelectElement);
    updateTypeSelect(registerChoices, "", "REGISTER");
    registerSelectElement.addEventListener("search",
        function(event) {
            updateTypeSelect(registerChoices, event.detail.value, "REGISTER");
        },
        false,
    );
    return registerChoices;
}

function initAssetSelectRisk(assetSelectElement) {
    const assetChoices = initSelect(assetSelectElement);
    updateTypeSelect(assetChoices, "", "ASSET");
    assetSelectElement.addEventListener("search",
        function(event) {
            updateTypeSelect(assetChoices, event.detail.value, "ASSET");
        },
        false,
    );
    return assetChoices;
}

function loadRegisterResponsible(selectedRegisterElement, userChoicesSelect) {
    let selectedRegister = selectedRegisterElement.value;
    fetch( `/rest/risks/register?registerId=${selectedRegister}`)
        .then(response => response.json()
            .then(data => {
                var user = data.users[0];
                if (user != null) {
                    userChoicesSelect.setChoiceByValue(user.uuid);
                } else {
                    userChoicesSelect.removeActiveItems();
                }

                if (data.elementName != null) {
                    document.getElementById('name').value = data.elementName;
                } else {
                    document.getElementById('name').value = "";
                }
            }))
        .catch(error => toastService.error(error));
}

const createRiskService = new CreateRiskService();
const copyRiskService = new CopyRiskService();
const editRiskService = new EditRiskService();
document.addEventListener("DOMContentLoaded", function(event) {
    createRiskService.init();
});

function EditRiskService() {
    this.getScopedElementById = function(id) {
        return this.modalContainer.querySelector(`#${id}`);
    }

    this.showEditDialog = function (threatAssessmentId) {
        const container = document.getElementById('editAssessmentContainer');
        fetch(`${baseUrl}${threatAssessmentId}/edit`)
            .then(response => response.text()
                .then(data => {
                    container.innerHTML = data;
                    this.onShown();
                })
            )
            .catch(error => toastService.error(error));
    }

    this.onShown = function() {
        let self = this;
        this.modalContainer = document.getElementById('editModal');

        const presentSelect = this.getScopedElementById('editPresentAtMeetingSelect');
        if (presentSelect !== null) {
            this.presentSelect = choiceService.initUserSelect('editPresentAtMeetingSelect');
        }

        this.userChoicesSelect = choiceService.initUserSelect("editUserSelect");
        this.ouChoicesSelect = choiceService.initOUSelect("editOuSelect");
        initFormValidationForForm("editRiskModalForm",
            () => this.validate());


        this.userChoicesSelect.passedElement.element.addEventListener('change', function() {
            const userUuid = self.userChoicesSelect.passedElement.element.value;
            self.userChanged(userUuid);
        });

        const assetSelect = this.getScopedElementById('copyAssetSelect');
        if (assetSelect !== null) {
            this.assetChoicesSelect = initAssetSelectRisk(assetSelect);
        }

        this.editAssessmentModal = new bootstrap.Modal(this.modalContainer);
        this.editAssessmentModal.show();
    }

    this.validate = function() {
        let result = validateChoices(this.userChoicesSelect, this.ouChoicesSelect);
        if (this.assetChoicesSelect != null) {
            result &= checkInputField(this.assetChoicesSelect, true);
        }
        return result;
    }
}

function CopyRiskService() {
    this.getScopedElementById = function(id) {
        return this.modalContainer.querySelector(`#${id}`);
    }

    this.showCopyDialog = function(threatAssessmentId) {
        const container = document.getElementById('copyAssessmentContainer');
        fetch(`${baseUrl}${threatAssessmentId}/copy`)
            .then(response => response.text()
                .then(data => {
                    container.innerHTML = data;
                    this.onShown();
                })
            )
            .catch(error => toastService.error(error));
    }

    this.onShown = function() {
        let self = this;
        this.modalContainer = document.getElementById('copyModal');
        const registerSelect = this.getScopedElementById('copyRegisterSelect');
        if (registerSelect !== null) {
            this.registerChoicesSelect = initRegisterSelect(registerSelect);
        }
        const assetSelect = this.getScopedElementById('copyAssetSelect');
        if (assetSelect !== null) {
            this.assetChoicesSelect = initAssetSelectRisk(assetSelect);
        }
        const presentSelect = this.getScopedElementById('copyPresentAtMeetingSelect');
        if (presentSelect !== null) {
            this.presentSelect = choiceService.initUserSelect('copyPresentAtMeetingSelect');
        }

        this.userChoicesSelect = choiceService.initUserSelect("copyUserSelect");
        this.ouChoicesSelect = choiceService.initOUSelect("copyOuSelect");
        initFormValidationForForm("copyRiskModalForm",
            () => this.validate());


        this.userChoicesSelect.passedElement.element.addEventListener('change', function() {
            const userUuid = self.userChoicesSelect.passedElement.element.value;
            self.userChanged(userUuid);
        });

        this.copyAssessmentModal = new bootstrap.Modal(this.modalContainer);
        this.copyAssessmentModal.show();
    }

    this.userChanged = function (userUuid) {
        fetch( `/rest/ous/user/` + userUuid)
            .then(response =>  response.json()
                .then(data => {
                    this.ouChoicesSelect.setChoices([data], 'uuid', 'name');
                    this.ouChoicesSelect.setChoiceByValue(data.uuid);
                })).catch(error => toastService.error(error));
    }

    this.validate = function() {
        let result = validateChoices(this.userChoicesSelect, this.ouChoicesSelect);
        if (this.assetChoicesSelect != null) {
            result &= checkInputField(this.assetChoicesSelect, true);
        } else if (this.registerChoicesSelect != null) {
            result &= validateChoices(this.registerChoicesSelect);
        }
        return result;
    }
}

function CreateRiskService() {

    this.init = function () {
        let self = this;
        this.modalContainer = document.getElementById('createModal');

        const registerSelect = this.getScopedElementById('registerSelect');
        const assetSelect = this.getScopedElementById('assetSelect');
        this.registerChoicesSelect = initRegisterSelect(registerSelect);
        this.assetChoicesSelect = initAssetSelectRisk(assetSelect);
        this.userChoicesSelect = choiceService.initUserSelect("userSelect");
        this.ouChoicesSelect = choiceService.initOUSelect("ouSelect");

        this.userChoicesSelect.passedElement.element.addEventListener('change', function() {
             const userUuid = self.userChoicesSelect.passedElement.element.value;
             self.userChanged(userUuid);
        });

        this.typeChanged(this.getScopedElementById("threatAssessmentType").value);
        this.getScopedElementById('threatAssessmentType').addEventListener('change', function() {
            self.typeChanged(this.value);
        });

        this.assetChoicesSelect.passedElement.element.addEventListener('change', function() {
            self.clearAssetValidationError();
            self.loadAssetSection();
        });
        let selectedRegisterElement = this.getScopedElementById("registerSelect");
        this.registerChoicesSelect.passedElement.element.addEventListener('change', function() {
            self.clearRegisterValidationError();
            loadRegisterResponsible(selectedRegisterElement, self.userChoicesSelect);
        });

        this.getScopedElementById('sendEmailcheckbox').addEventListener('change', function() {
            self.sendEmailChanged(this.checked);
        });

        const presentSelect = this.getScopedElementById('presentAtMeetingSelect');
        if (presentSelect !== null) {
            this.presentSelect = choiceService.initUserSelect('presentAtMeetingSelect');
        }

        let societyCheckbox = this.getScopedElementById("society");
        let authenticityCheckbox = this.getScopedElementById("authenticity");
        let authenticitySection = this.getScopedElementById("authenticitySection");
        societyCheckbox.addEventListener('change', function() {
            if (societyCheckbox.checked) {
                // show authenticity checkbox
                authenticitySection.hidden = false;
            } else {
                // hide authenticity checkbox and reset
                authenticitySection.hidden = true;
                authenticityCheckbox.checked = false;
            }
        });

        initFormValidationForForm("createRiskModal",
            () => {
                return this.validateEntitySelection() &&
                    this.validateChoicesAndCheckboxesRisk(this.userChoicesSelect, this.ouChoicesSelect);
            });

    }

    this.typeChanged = function (selectedType) {
        if (selectedType === 'ASSET') {
            this.getScopedElementById("registerSelectRow").style.display = 'none';
            this.getScopedElementById("assetSelectRow").style.display = '';
        } else if (selectedType === 'REGISTER') {
            this.getScopedElementById("registerSelectRow").style.display = '';
            this.getScopedElementById("assetSelectRow").style.display = 'none';
        } else {
            this.getScopedElementById("registerSelectRow").style.display = 'none';
            this.getScopedElementById("assetSelectRow").style.display = 'none';
        }
        this.getScopedElementById("inheritRow").style.display = 'none';
        this.registerChoicesSelect.removeActiveItems();
        this.assetChoicesSelect.removeActiveItems();
        this.selectedType = selectedType;
    }

    this.userChanged = function (userUuid) {
        fetch( `/rest/ous/user/` + userUuid)
            .then(response =>  response.json()
                .then(data => {
                    this.ouChoicesSelect.setChoices([data], 'uuid', 'name');
                    this.ouChoicesSelect.setChoiceByValue(data.uuid);
                })).catch(error => toastService.error(error));
    }

    this.clearRegisterValidationError = function() {
        this.getScopedElementById("registerSelect").parentElement.classList.remove('is-invalid');
        this.getScopedElementById("registerError").classList.remove('show');
    }

    this.clearAssetValidationError = function() {
        this.getScopedElementById("assetSelect").parentElement.classList.remove('is-invalid');
        this.getScopedElementById("assetError").classList.remove('show');
    }

    this.validateEntitySelection = function () {
        let result = true;
        if (this.selectedType === "ASSET") {
            // Check that at least one asset is selected
            let assetSelect = this.getScopedElementById("assetSelect");
            let assetSelected = assetSelect.value !== "";
            if (assetSelected) {
                this.clearAssetValidationError();
            } else {
                assetSelect.parentElement.classList.add('is-invalid');
                this.getScopedElementById("assetError").classList.add('show');
            }
            result &= assetSelected;
        } else if (this.selectedType === 'REGISTER') {
            let registerSelect = this.getScopedElementById("registerSelect");
            let registerSelected = registerSelect.value !== "";
            if (registerSelected) {
                this.clearRegisterValidationError();
            } else {
                registerSelect.parentElement.classList.add('is-invalid');
                this.getScopedElementById("registerError").classList.add('show');
            }
            result &= registerSelected;
        }
        return result;
    };

    this.validateChoicesAndCheckboxesRisk = function (...choiceList) {
        let result = true;
        for (let i = 0; i < choiceList.length; i++) {
            if (!checkInputField(choiceList[i])) {
                result = false;
            }
        }
        let registered = this.getScopedElementById("registered");
        let organisation = this.getScopedElementById("organisation");
        let society = this.getScopedElementById("society");
        if (!registered.checked && !organisation.checked && !society.checked) {
            registered.classList.add('is-invalid');
            organisation.classList.add('is-invalid');
            this.getScopedElementById("checkboxError").classList.add('show');
            result = false;
        } else {
            registered.classList.remove('is-invalid');
            organisation.classList.remove('is-invalid');
            this.getScopedElementById("checkboxError").classList.remove('show');
        }
        return result;
    }

    this.loadAssetSection = function () {
        const selectedAsset = this.getScopedElementById("assetSelect").value;
        fetch( `/rest/risks/asset?assetIds=${selectedAsset}`)
            .then(response => response.json()
                .then(data => {
                    let user = data.users?.users[0];
                    if (user != null) {
                        this.userChoicesSelect.setChoiceByValue(user.uuid);
                    } else {
                        this.userChoicesSelect.removeActiveItems();
                    }

                    if (data.elementName != null) {
                        this.getScopedElementById('name').value = data.elementName;
                    } else {
                        this.getScopedElementById('name').value = "";
                    }

                    // set text in table
                    this.getScopedElementById("RF").innerHTML = data.rf === 0 ? "" : data.rf;
                    this.getScopedElementById("OF").innerHTML = data.of === 0 ? "" : data.of;
                    this.getScopedElementById("SF").innerHTML = data.sf === 0 ? "" : data.sf;
                    this.getScopedElementById("RI").innerHTML = data.ri === 0 ? "" : data.ri;
                    this.getScopedElementById("OI").innerHTML = data.oi === 0 ? "" : data.oi;
                    this.getScopedElementById("SI").innerHTML = data.si === 0 ? "" : data.si;
                    this.getScopedElementById("RT").innerHTML = data.rt === 0 ? "" : data.rt;
                    this.getScopedElementById("OT").innerHTML = data.ot === 0 ? "" : data.ot;
                    this.getScopedElementById("ST").innerHTML = data.st === 0 ? "" : data.st;

                    this.getScopedElementById("inheritRow").style.display = '';
                }))
            .catch(error => toastService.error(error));
    }

    this.sendEmailChanged = function (checked) {
        this.getScopedElementById('sendEmail').value = checked;
    }

    this.getScopedElementById = function(id) {
        return this.modalContainer.querySelector(`#${id}`);
    }


}
