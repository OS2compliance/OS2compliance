class CreateDPIAService {
    assetChoicesSelect
    formElement

    constructor () {

    }

    init() {
        const assetSelect = document.getElementById('assetSelect');
        this.assetChoicesSelect = this.#initAssetSelect(assetSelect);

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
        return assetChoices;
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
        const assetSelect = document.getElementById('assetSelect');
        const userUpdatedDateElement = document.getElementById('createUserUpdateDateField')
        const data = {
            assetId : assetSelect.value,
            userUpdatedDate: userUpdatedDateElement.value,
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

        location.reload()
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