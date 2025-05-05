class CreateDPIAService {
    assetChoicesSelect
    formElement

    constructor () {

    }

    init() {
        const assetSelect = document.getElementById('assetSelect');
        this.assetChoicesSelect = this.#initAssetSelect(assetSelect);
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
        const data = {
            assetId : assetSelect.value
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

}