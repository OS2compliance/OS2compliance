function initNotificationSelect(checkboxId, selectDivId, selectInputId) {
    let notificationSelect = null;

    const checkbox = document.getElementById(checkboxId);
    const selectDiv = document.getElementById(selectDivId);

    if (!checkbox || !selectDiv) {
        console.warn(`Notification select elements not found: ${checkboxId}, ${selectDivId}`);
        return null;
    }

    const showOrHide = (isChecked) => {
        if (isChecked) {
            selectDiv.hidden = false;

            // Initialize Choices.js only when first shown
            if (!notificationSelect) {
                notificationSelect = new Choices(`#${selectInputId}`, {
                    removeItemButton: true,
                    searchEnabled: true,
                });
            }
        } else {
            selectDiv.hidden = true;
            if (notificationSelect) {
                notificationSelect.removeActiveItems();
            }
        }
    };

    // Set up event listener
    checkbox.addEventListener("change", (event) => {
        showOrHide(event.target.checked);
    });

    // Trigger initial state
    checkbox.dispatchEvent(new Event("change"));

    return {
        get choicesInstance() {
            return notificationSelect;
        },
        enable: () => notificationSelect?.enable(),
        disable: () => notificationSelect?.disable(),
        removeActiveItems: () => notificationSelect?.removeActiveItems()
    };
}

// Make it globally available
window.initNotificationSelect = initNotificationSelect;