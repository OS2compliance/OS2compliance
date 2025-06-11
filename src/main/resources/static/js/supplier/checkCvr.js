
function checkCvr(cvr) {
    if (cvr.value.length !== 8) {
        return;
    }
    const inputName = document.querySelector('input[id="navn"]');
    const inputAdresse = document.querySelector('input[id="adresse"]');
    const inputPostNr = document.querySelector('input[id="postnr"]');
    const inputBy = document.querySelector('input[id="by"]');
    const inputLand = document.querySelector('input[id="land"]');
    const inputTelefon = document.querySelector('input[id="telefon"]');
    const inputMail = document.querySelector('input[id="mail"]');
    let cvrNum = Number(cvr.value);
    if (isNaN(cvrNum)) {
        return;
    }
    fetch( "/rest/cvr?cvr=" + cvr.value)
    .then(response => response.json()
        .then(json => {
            if (json.name) {
                inputName.value = json.name;
            }
            if (json.address) {
                inputAdresse.value = json.address;
            }
            if (json.zipCode) {
                inputPostNr.value = json.zipCode;
            }
            if (json.city) {
                inputBy.value = json.city;
            }
            if (json.country) {
                inputLand.value = json.country;
            }
            if (json.phone) {
                inputTelefon.value = json.phone;
            }
            if (json.email) {
                inputMail.value = json.email;
            }
        })
    )
    .catch(reason => toastService.error(reason));
}
