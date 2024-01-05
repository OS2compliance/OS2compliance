
const settingsService = new SettingsService();

function SettingsService() {

    this.show = function() {
        this.loadSettingElement()
            .then(() => {
                let modal = document.querySelector('#settingsDialog');
                const settingsModal = new bootstrap.Modal(modal);
                settingsModal.show();
            })
    }

    this.loadSettingElement = function (){
        return fetch(`/settings/form`)
            .then(response => response.text()
                .then(data => {
                    document.getElementById('settings').innerHTML = data;
                    this.initKitosSelect('ownerSelect');
                    this.initKitosSelect('responsibleSelect');
                })
            ).catch(error => toastService.error(error));
    }


    this.initKitosSelect = function (elementId) {
        const select = document.getElementById(elementId);
        if (select === null) {
            return;
        }
        const choices = initSelect(select);
        this.updateKitos(choices, "");
        const self = this;
        select.addEventListener("search",
            function (event) {
                self.updateKitos(choices, event.detail.value)
            },
            false
        );
        return choices;
    }

    this.updateKitos = function (targetChoice, search) {
        fetch( `/rest/kitos/autocomplete?search=${search}`)
            .then(response => response.json()
                .then(data => {
                    targetChoice.setChoices(data.content.map(e => {
                        return {
                            value: e.uuid,
                            label: `${e.name}`}
                    }), 'value', 'label', true);
                }))
            .catch(error => toastService.error(error));
    }


}
