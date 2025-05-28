class CreateExternalDPIAService {
    assetChoicesSelect
    formElement
    responsibleUserChoice = null
    responsibleOUChoice = null

    constructor () {

    }

    init() {
        const assetSelect = document.getElementById('externalDPIAAssetSelect');
        this.assetChoicesSelect = this.#initAssetSelect(assetSelect);
        this.#initSearchOus('externalOuSelect')
        this.#initSearchUsers('externalUserSelect')

        const datepickerElement = document.getElementById('externalUserUpdateDateField')
        this.initDatePicker('externalUserUpdateDateField', datepickerElement.value)
    }

    formReset() {
        this.formElement = document.getElementById('createExternalDPIAForm');
        this.formElement.reset();
    }

    #initAssetSelect(assetSelectElement){
        if (assetSelectElement !== null) {

            const assetChoices = initSelect(assetSelectElement);
            this.#updateTypeSelect(assetChoices, "", "ASSET");
            assetSelectElement.addEventListener("search",
                function(event) {
                    this.#updateTypeSelect(assetChoices, event.detail.value, "ASSET");
                },
                false,
            );

            //update title if empty
            assetSelectElement.addEventListener('change', (event) => {
                const selected = event.target.selectedOptions
                const titleElement = document.getElementById('externalTitleInput')
                if (titleElement.value === null
                    || titleElement.value === '') {
                    if (selected.length > 1) {
                        titleElement.value = 'Konsekvensanalyse for ' + selected[0].textContent.replace("Aktiv: ", "") + ' med flere'
                    } else if (selected.length === 1) {
                        titleElement.value = 'Konsekvensanalyse for ' + selected[0].textContent.replace("Aktiv: ", "")
                    }
                }
            })

            return assetChoices;
        }
    }

    #updateTypeSelect(choices, search, types) {
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

    async submitNewExternalDPIA(dpiaId) {
        const titleInput = document.getElementById('externalTitleInput');
        const assetSelect = document.getElementById('externalDPIAAssetSelect');
        const linkInput = document.getElementById('linkInput');
        const userUpdatedDateElement = document.getElementById('externalUserUpdateDateField')
        const data = {
            dpiaId: dpiaId ? dpiaId : null,
            assetIds : assetSelect ? [...assetSelect.selectedOptions].map(o => o.value) : null,
            link: linkInput.value ? linkInput.value : "",
            userUpdatedDate: userUpdatedDateElement.value,
            responsibleUserUuid: this.responsibleUserChoice.getValue(true),
            responsibleOuUuid: this.responsibleOUChoice.getValue(true),
            title: titleInput.value,
        }

        const url = `${restUrl}/external/create`
        const response = await fetch(url, {
            method: "POST",
            headers: {
                'X-CSRF-TOKEN': token,
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        })

        if (!response.ok)  {
            toastService.error(response.statusText)
        }

        location.reload()
    }

    async editExternalClicked(dpiaId) {
        try {
            const url = `${baseUrl}/external/${dpiaId}/edit`
            const response = await fetch(url, {
                method: "GET",
                headers: {
                    'X-CSRF-TOKEN': token
                }
            })

            if (!response.ok) {
                toastService.error(response.statusText)
                throw Error("Error in getting editing view for external dpia: " + dpiaId)
            }

            const responseText = await response.text()

            const externalModalContainer = document.getElementById("external_modal_container")
            externalModalContainer.innerHTML = responseText

            this.init()

            const modalElement = externalModalContainer.querySelector('#createExternalDPIAModal')
            const modal = new bootstrap.Modal(modalElement);
            modal.show();
        } catch (e) {
            console.error("could not open edit view:\n" + e.message)
        }
    }

    async createExternalClicked() {
        const url = `${baseUrl}/external/create`
        const response = await fetch(url, {
            method: "GET",
            headers: {
                'X-CSRF-TOKEN': token
            }
        })

        if (!response.ok)  {
            toastService.error(response.statusText)
        }

        const responseText = await response.text()

        const externalModalContainer = document.getElementById("external_modal_container")
        externalModalContainer.innerHTML = responseText

        this.init()

        const modalElement = externalModalContainer.querySelector('#createExternalDPIAModal')
        const modal = new bootstrap.Modal(modalElement);
        modal.show();
    }

    initDatePicker(id, selectedDate) {
        let date = new Date()
        if (selectedDate) {
            const [day, rest] = selectedDate ? selectedDate.split('/') : [null, null];
            const [month, year] = rest ? rest.split('-') : [null, null];
            date = new Date(parseInt(year), parseInt(month) - 1, parseInt(day))
        }

        return MCDatepicker.create({
            el: `#${id}`,
            autoClose: true,
            dateFormat: 'dd/mm-yyyy',
            closeOnBlur: true,
            firstWeekday: 1,
            customWeekDays: ["sø", "ma", "ti", "on", "to", "fr", "lø"],
            customMonths: ["Januar", "Februar", "Marts", "April", "Maj", "Juni", "Juli", "August", "September", "Oktober", "November", "December"],
            customClearBTN: "Ryd",
            customCancelBTN: "Annuller",
            selectedDate: date,
        });
    }

    #initSearchOus(elementId){
        this.responsibleOUChoice = choiceService.initOUSelect(elementId)
    }

    #initSearchUsers(elementId){
        this.responsibleUserChoice = choiceService.initUserSelect(elementId)
    }

}
