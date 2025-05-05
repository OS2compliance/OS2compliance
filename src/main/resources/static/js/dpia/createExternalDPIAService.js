class CreateExternalDPIAService {
    assetChoicesSelect
    formElement
    externalModalContainer

    constructor () {

    }

    init() {
        const assetSelect = document.getElementById('externalDPIAAssetSelect');
        this.assetChoicesSelect = this.#initAssetSelect(assetSelect);
    }

    formReset() {
        this.formElement = document.getElementById('createExternalDPIAForm');
        this.formElement.reset();
    }


    #initAssetSelect(assetSelectElement) {
        const assetChoices = initSelect(assetSelectElement);
        this.#updateTypeSelect(assetChoices, "", "ASSET");
        assetSelectElement.addEventListener("search",
            function(event) {
                this.#updateTypeSelect(assetChoices, event.detail.value, "ASSET");
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

    async submitNewExternalDPIA(dpiaId) {
        const assetSelect = document.getElementById('externalDPIAAssetSelect');
        const linkInput = document.getElementById('linkInput');
        const data = {
            dpiaId: dpiaId ? dpiaId : null,
            assetId : assetSelect ? assetSelect.value : null,
            link: linkInput.value ? linkInput.value : ""
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
        const url = `${baseUrl}/external/${dpiaId}/edit`
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

        const modalElement = externalModalContainer.querySelector('#createExternalDPIAModal')
        const modal = new bootstrap.Modal(modalElement);
        modal.show();

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

}
