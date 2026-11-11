package com.giathinh.uthconnect.presentation.housing.create

import com.giathinh.uthconnect.domain.model.GenderPreference
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.RoomAmenity
import com.giathinh.uthconnect.domain.model.UthCampus

sealed interface CreateHousingPostAction {
    data class ChangeType(val type: HousingListingType) : CreateHousingPostAction
    data class ChangeCampus(val campus: UthCampus) : CreateHousingPostAction
    data class ChangeTitle(val title: String) : CreateHousingPostAction
    data class ChangeDescription(val desc: String) : CreateHousingPostAction
    data class ChangeRent(val rent: String) : CreateHousingPostAction
    data class ChangeDeposit(val deposit: String) : CreateHousingPostAction
    data class ChangeUtilitiesIncluded(val included: Boolean) : CreateHousingPostAction
    data class ChangeAreaLabel(val areaLabel: String) : CreateHousingPostAction
    data class ChangeExactAddress(val exactAddress: String) : CreateHousingPostAction
    data class ChangeArea(val area: String) : CreateHousingPostAction
    data class ChangeSlots(val slots: Int) : CreateHousingPostAction
    data class ChangeGender(val gender: GenderPreference) : CreateHousingPostAction
    data class ChangeScheduleNote(val note: String) : CreateHousingPostAction
    data class ToggleAmenity(val amenity: RoomAmenity) : CreateHousingPostAction
    data class ChangeContactPhone(val phone: String) : CreateHousingPostAction
    data class ChangeContactZalo(val zalo: String) : CreateHousingPostAction
    data class ChangeAllowZalo(val allowed: Boolean) : CreateHousingPostAction
    data class AddPhoto(val uri: String) : CreateHousingPostAction
    data class RemovePhoto(val index: Int) : CreateHousingPostAction
    data object SaveDraft : CreateHousingPostAction
    data object Submit : CreateHousingPostAction
}
