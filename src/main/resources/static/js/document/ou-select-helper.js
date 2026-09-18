export function selectOu(ouChoicesSelect, ou) {
    ouChoicesSelect.setChoiceByValue(ou.uuid);
    if (ouChoicesSelect.getValue(true) === ou.uuid) {
        return;
    }
    ouChoicesSelect.setChoices([{ value: ou.uuid, label: ou.name, selected: true }], 'value', 'label', false);
}
