import SwiftUI
import UIKit

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
    @AppStorage(SharedSettings.Key.sound, store: SharedSettings.group)
    private var sound = SharedSettings.defaultSound
    @AppStorage(SharedSettings.Key.vibrate, store: SharedSettings.group)
    private var vibrate = SharedSettings.defaultVibrate
    @AppStorage(SharedSettings.Key.clipboardHistory, store: SharedSettings.group)
    private var clipboardHistory = true
    @State private var confirmClear = false
    @Environment(\.openURL) private var openURL

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

            Divider().padding(.top, 14)

            Toggle(isOn: $sound) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Sound on key press")
                        .font(.subheadline.weight(.medium))
                    // playInputClick() is silent unless the system setting is on as well
                    Text("When keyboard sound is on in iPhone Settings")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .tint(Color.crimson)
            .padding(.top, 14)

            Divider().padding(.top, 14)

            Toggle(isOn: $vibrate) {
                Text("Vibrate on key press")
                    .font(.subheadline.weight(.medium))
            }
            .tint(Color.crimson)
            .padding(.top, 14)

            Divider().padding(.top, 14)

            clipboard
        }
        .onChange(of: keySize) { _ in onChange() }
        .onChange(of: smartSuggestions) { _ in onChange() }
        .onChange(of: sound) { _ in onChange() }
        .onChange(of: vibrate) { _ in onChange() }
        .onChange(of: clipboardHistory) { _ in onChange() }
    }

    /**
     * Clipboard history is the one feature that needs Full Access: without it iOS hides the
     * pasteboard from keyboards. The app can't tell whether it's on, so the row just opens the
     * app's page in iPhone Settings (Keyboards → Allow Full Access).
     */
    @ViewBuilder
    private var clipboard: some View {
        Toggle(isOn: $clipboardHistory) {
            VStack(alignment: .leading, spacing: 2) {
                Text("Clipboard history")
                    .font(.subheadline.weight(.medium))
                Text("Needs Full Access. Kept on this iPhone only.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .tint(Color.crimson)
        .padding(.top, 14)

        Divider().padding(.top, 14)

        Button {
            if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
        } label: {
            HStack {
                Text("Full Access")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(.primary)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .padding(.top, 14)

        Divider().padding(.top, 14)

        Button("Clear clipboard history", role: .destructive) { confirmClear = true }
            .font(.subheadline.weight(.medium))
            .padding(.top, 14)
            .alert("Clear clipboard history?", isPresented: $confirmClear) {
                Button("Clear", role: .destructive) { SharedSettings.clearClipboardHistory() }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("This removes every saved item, including pinned ones.")
            }
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
