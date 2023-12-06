
function formReset() {
    const form = document.querySelector('form');
    form.reset();
}

function formLoaded() {
    const ouSelect = document.getElementById('ouSelect');
    const ouChoices = new Choices(ouSelect, {
        searchChoices: false,
        allowHTML: true,
        searchPlaceholderValue: 'Søg flere....',
        classNames: {
            containerInner: 'form-control'
        }
    });
    ouSelect.addEventListener("search",
        function(event) {
            updateOus(ouChoices, event.detail.value);
        },
        false,
    );
    const userSelect = document.getElementById('userSelect');
    const userChoices = new Choices(userSelect, {
        searchChoices: false,
        allowHTML: true,
        searchPlaceholderValue: 'Søg flere....',
        classNames: {
            containerInner: 'form-control'
        }
    });
    userSelect.addEventListener("search",
        function(event) {
            updateUsers(userChoices, event.detail.value);
        },
        false,
    );

    updateOus(ouChoices, "");
    updateUsers(userChoices, "");
}
