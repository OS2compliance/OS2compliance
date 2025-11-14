

export function initColorPickerListener(colorPickerId) {
    if (!colorPickerId) {
        console.error("Could not initialize colorPicker. No Id found")
    }
    const picker = document.getElementById(colorPickerId);

    new ColorPickerIndicator(picker);
}

class ColorPickerIndicator {
    picker

    constructor (picker) {
        if (!picker) {
            console.error("Could not initialize colorPicker. No Id found")
        }
        this.picker = picker

        this.initIndicator()
        this.updateColorIndicator()

        picker.addEventListener("change", (e) => {
            this.updateColorIndicator(picker)
        })
    }

    initIndicator() {
        const indicator = document.createElement("span")
        indicator.className = "tag-badge color-picker-indicator"

        this.picker.parentElement.appendChild(indicator)
    }

    updateColorIndicator() {
        const selectedOptions = [...this.picker.selectedOptions]
        let selectedOption;

        if (selectedOptions.length > 0) {
            selectedOption = selectedOptions[0];
        }

        const indicator = this.picker.parentElement.querySelector('.color-picker-indicator');
        if (selectedOption) {
            // set style from option
            indicator.style.backgroundColor = selectedOption.style.backgroundColor;
            indicator.style.color = selectedOption.style.color
        } else {
            // remove background styling
            indicator.style.backgroundColor = 'transparent';
            indicator.style.color = 'grey'
        }
    }
}

