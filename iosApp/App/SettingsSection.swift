import SwiftUI

/**
 * The keyboard's settings, stored in the App Group for the keyboard to read when it next opens.
 * [onChange] runs after each change (the screen reopens the keyboard to show it).
 */
struct SettingsSection: View {
    var onChange: () -> Void = {}

    @AppStorage(SharedSettings.Key.nepaliStyle, store: SharedSettings.group)
    private var style: NepaliStyle = .romanized
    @AppStorage(SharedSettings.Key.keyHeight, store: SharedSettings.group)
    private var keySize: KeySize = .normal
    @AppStorage(SharedSettings.Key.smartSuggestions, store: SharedSettings.group)
    private var smartSuggestions = true

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Settings")
                .font(.title3.weight(.semibold))

            Text("Nepali typing")
                .font(.subheadline.weight(.medium))
                .padding(.top, 20)
            Picker("Nepali typing", selection: styleBinding) {
                Text("Romanized").tag(NepaliStyle.romanized)
                Text("Traditional").tag(NepaliStyle.traditional)
            }
            .pickerStyle(.segmented)
            .labelsHidden()
            .padding(.top, 8)

            Text("Key height")
                .font(.subheadline.weight(.medium))
                .padding(.top, 20)
            Picker("Key height", selection: $keySize) {
                Text("Short").tag(KeySize.short)
                Text("Normal").tag(KeySize.normal)
                Text("Tall").tag(KeySize.tall)
            }
            .pickerStyle(.segmented)
            .labelsHidden()
            .padding(.top, 8)

            Divider().padding(.top, 20)

            Toggle(isOn: $smartSuggestions) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Smart suggestions")
                        .font(.subheadline.weight(.medium))
                    Text("Dates, amounts and units as you type")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .tint(Color.crimson)
            .padding(.top, 14)
        }
        .onChange(of: keySize) { _ in onChange() }
        .onChange(of: smartSuggestions) { _ in onChange() }
    }

    /** Also stamps the time, so this choice beats an older one made in the keyboard's menu. */
    private var styleBinding: Binding<NepaliStyle> {
        Binding(
            get: { style },
            set: { newValue in
                style = newValue
                SharedSettings.group.set(Date().timeIntervalSince1970, forKey: SharedSettings.Key.nepaliStyleTime)
                onChange()
            }
        )
    }
}

#Preview {
    SettingsSection().padding(24)
}
