class CreateDPIAService {
    assetChoicesSelect
    formElement

    constructor () {

    }

    init() {
        const assetSelect = document.getElementById('assetSelect');
        this.assetChoicesSelect = this.#initAssetSelect(assetSelect);
        this.#initSearchOus('ouSelect')
        this.#initSearchUsers('userSelect')

        const datepickerElement = document.getElementById('createUserUpdateDateField')
        const userUpdatedDateDatepicker = this.initDatePicker('createUserUpdateDateField')
        datepickerElement.addEventListener('click', ()=> {
            userUpdatedDateDatepicker.open()
        })
    }

    formReset() {
        this.formElement = document.getElementById('createDPIAForm');
        this.formElement.reset();
    }


    #initAssetSelect(assetSelectElement) {
        const self = this;
        const assetChoices = initSelect(assetSelectElement);
        this.#updateTypeSelect(assetChoices, "", "ASSET");
        assetSelectElement.addEventListener("search",
            function(event) {
                self.#updateTypeSelect(assetChoices, event.detail.value, "ASSET");
            },
            false,
        );

        //update title if empty
        assetSelectElement.addEventListener('change', (event)=> {
            const selected = event.target.selectedOptions
            const titleElement = document.getElementById('createTitleInput')
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

    #initSearchOus(elementId){
        choiceService.initOUSelect(elementId)
    }

    #initSearchUsers(elementId){
        choiceService.initUserSelect(elementId)
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

    async submitNewDPIA() {
        const titleInput = document.getElementById('createTitleInput');
        const assetSelect = document.getElementById('assetSelect');
        const userUpdatedDateElement = document.getElementById('createUserUpdateDateField')
        const userSelect = document.getElementById('userSelect');
        const ouSelect = document.getElementById('ouSelect');
        const data = {
            assetIds : assetSelect ? [...assetSelect.selectedOptions].map(o => o.value) : null,
            userUpdatedDate: userUpdatedDateElement.value,
            responsibleUserUuid: userSelect.value,
            responsibleOuUuid: ouSelect.value,
            title: titleInput.value,
        }

        const url = `${restUrl}/create`
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

        const responseJson = await response.json()
        if (responseJson.dpiaId) {
            location.href = baseUrl+`/${responseJson.dpiaId}`
        } else {
            location.reload()
        }
    }

    initDatePicker(id) {
        return MCDatepicker.create({
            el: `#${id}`,
            autoClose: true,
            dateFormat: 'dd/mm-yyyy',
            closeOnBlur: true,
            firstWeekday: 1,
            customWeekDays: ["sø", "ma", "ti", "on", "to", "fr", "lø"],
            customMonths: ["Januar", "Februar", "Marts", "April", "Maj", "Juni", "Juli", "August", "September", "Oktober", "November", "December"],
            customClearBTN: "Ryd",
            customCancelBTN: "Annuller"
        });
    }

}