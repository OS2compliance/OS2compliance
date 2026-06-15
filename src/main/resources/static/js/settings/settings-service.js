
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
                    this.initKitosSelect('otherSelect');
                    this.initDbsRecipientSelector();
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

    this.initDbsRecipientSelector = function() {
        const wrapper = document.getElementById('dbsRecipientWrapper');
        if (!wrapper) return;

        const hiddenValue = wrapper.querySelector('[data-ref="dbsRecipientValue"]');
        const typeSelect = wrapper.querySelector('[data-ref="dbsRecipientType"]');
        const roleSelect = wrapper.querySelector('[data-ref="dbsRecipientRole"]');
        const emailInput = wrapper.querySelector('[data-ref="dbsRecipientEmail"]');

        // Initialize UI from current stored value
        const currentValue = hiddenValue.value;
        if (currentValue.startsWith('ROLE:')) {
            typeSelect.value = 'role';
            roleSelect.value = currentValue;
            roleSelect.classList.remove('d-none');
        } else if (currentValue.length > 0) {
            typeSelect.value = 'email';
            emailInput.value = currentValue;
            emailInput.classList.remove('d-none');
        }

        const syncValue = () => {
            if (typeSelect.value === 'role') {
                hiddenValue.value = roleSelect.value;
            } else if (typeSelect.value === 'email') {
                hiddenValue.value = emailInput.value;
            } else {
                hiddenValue.value = '';
            }
        };

        typeSelect.addEventListener('change', () => {
            roleSelect.classList.toggle('d-none', typeSelect.value !== 'role');
            emailInput.classList.toggle('d-none', typeSelect.value !== 'email');
            syncValue();
        });

        roleSelect.addEventListener('change', syncValue);
        emailInput.addEventListener('input', syncValue);
    };
}
