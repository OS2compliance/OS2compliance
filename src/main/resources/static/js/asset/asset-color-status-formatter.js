/**
 * Shared Grid.js formatter for the color-coded status columns on the asset list
 * (Kategori, Vurdering af foranstaltninger, Risikovurdering fravalgt, DPIA, TIA vurdering).
 * All of these render one of: a color word (Rød/Orange/Gul/Grøn/Lysgrøn/Hvid/Grå) or "Fravalgt".
 */
const badgeClassByValue = {
    'Fravalgt': 'bg-secondary',
    'Rød': 'bg-red',
    'Orange': 'bg-orange',
    'Gul': 'bg-yellow-500',
    'Grøn': 'bg-green',
    'Lysgrøn': 'bg-green-300',
    'Hvid': 'bg-light text-dark border',
    'Grå': 'bg-secondary'
};

export function formatColorStatus(cell) {
    if (!cell || !badgeClassByValue.hasOwnProperty(cell)) {
        return '';
    }

    return gridjs.html(`<div class="d-block badge ${badgeClassByValue[cell]}" style="width: 90px">${cell}</div>`, 'div');
}
