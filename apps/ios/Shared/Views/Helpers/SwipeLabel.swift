//
//  SwipeLabel.swift
//  SimpleX (iOS)
//
//  Created by Levitating Pineapple on 06/08/2024.
//  Copyright © 2024 SimpleX Chat. All rights reserved.
//

import SwiftUI

struct SwipeLabel: View {
    private let text: String
    private let systemImage: String

    init(_ text: String, systemImage: String, inverted _: Bool) {
        self.text = text
        self.systemImage = systemImage
    }

    var body: some View {
        Label(text, systemImage: systemImage)
    }
}
