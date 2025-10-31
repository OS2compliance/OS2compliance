export default function initTagSelect (id) {
    const tagsSelect = document.getElementById(id);
    const tagsChoice = initSelect(tagsSelect);
    updateTags(tagsChoice, "");
    tagsSelect.addEventListener("search",
        function(event) {
            updateTags(tagsChoice, event.detail.value);
        },
        false,
    );
    tagsSelect.addEventListener("change",
        function(event) {
            updateTags(tagsChoice, "");
        },
        false,
    );
}

function updateTags (choices, search) {
    fetch( `/rest/relatable/tags/autocomplete?search=${search}`)
        .then(response => response.json()
            .then(data => {
                choices.setChoices(data.content.map(reg => {
                    return {
                        id: reg.id + "",
                        name: reg.value
                    }
                }), 'id', 'name', true);
            }))
        .catch(error => toastService.error(error));
}