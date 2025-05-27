class EditDPIAService {
    assetSelectElementId = 'dpia_edit_asset_select'
    editModalContainerID = 'edit_dpia_modal_container'
    editModalId = 'editDPIAModal'

    constructor() {}

    async onSubmit(dpiaId) {
        const titleInput = document.getElementById('editTitleInput');
        const assetSelect = document.getElementById(this.assetSelectElementId);
        const userUpdatedDateElement = document.getElementById('editUserUpdateDateField')
        const userSelect = document.getElementById('userSelect');
        const ouSelect = document.getElementById('ouSelect');
        const data = {
            assetIds : assetSelect ? [...assetSelect.selectedOptions].map(o => o.value) : null,
            userUpdatedDate: userUpdatedDateElement.value,
            responsibleUserUuid: userSelect.value,
            responsibleOuUuid: ouSelect.value,
            title: titleInput.value,
        }

        const url = `${restUrl}/${dpiaId}/edit`
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

    #initAssetSelection(){
        const assetSelectElement = document.getElementById(this.assetSelectElementId);
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
            const titleElement = document.getElementById('editTitleInput')
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

    async #fetchModalContent(dpiaId) {
        const url = `${baseUrl}/${dpiaId}/edit`
        const response = await fetch(url, {
            method: "GET",
            headers: {
                'X-CSRF-TOKEN': token
            }
        })

        if (!response.ok)  {
            toastService.error(response.statusText)
            throw Error("Error in getting editing view for dpia: "+dpiaId)
        }

        const responseText = await response.text()

        const externalModalContainer = document.getElementById(this.editModalContainerID)
        externalModalContainer.innerHTML = responseText


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

    async openModal(dpiaId) {
        try {
            await this.#fetchModalContent(dpiaId)

            this.#initAssetSelection()
            const datepickerElement = document.getElementById('editUserUpdateDateField')
            this.initDatePicker('editUserUpdateDateField', datepickerElement.value || '')

            this.#initSearchOus('editOuSelect')
            this.#initSearchUsers('editUserSelect')

            const modalElement = document.getElementById(this.editModalId)
            const modal = new bootstrap.Modal(modalElement);
            modal.show();
        } catch (e) {
            console.error("could not open edit view:\n" + e.message)
        }
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
        choiceService.initOUSelect(elementId)
    }

    #initSearchUsers(elementId){
        choiceService.initUserSelect(elementId)
    }

}