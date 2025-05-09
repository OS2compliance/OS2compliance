class EditDPIAService {
    assetSelectElementId = 'dpia_edit_asset_select'
    editModalContainerID = 'edit_dpia_modal_container'
    editModalId = 'editDPIAModal'

    constructor() {}

    async onSubmit(dpiaId) {
        const titleInput = document.getElementById('titleInput');
        const assetSelect = document.getElementById(this.assetSelectElementId);
        const data = {
            title: titleInput.value,
            assetId : assetSelect.value
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
        await this.#fetchModalContent(dpiaId)

        this.#initAssetSelection()

        const modalElement = document.getElementById(this.editModalId)
        console.log(modalElement)
        const modal = new bootstrap.Modal(modalElement);
        modal.show();
    }

}