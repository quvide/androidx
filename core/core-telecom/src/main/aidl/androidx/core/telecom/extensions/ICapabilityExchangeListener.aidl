/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.telecom.extensions;

import androidx.core.telecom.extensions.Capability;

@JavaPassthrough(annotation="@androidx.core.telecom.util.ExperimentalAppActions")
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
oneway interface ICapabilityExchangeListener {
    // Called to complete the ICapabilityExchange#negotiateCapabilities request with the
    // capabilities that both the VOIP service and InCallService support.
    void onCapabilitiesNegotiated(in List<Capability> filteredCapabilities) = 0;
}