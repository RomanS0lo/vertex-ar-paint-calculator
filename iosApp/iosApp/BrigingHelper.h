// iosApp/iosApp/BridgingHeader.h
// If you need a bridging header for Objective-C compatibility

#ifndef BridgingHeader_h
#define BridgingHeader_h

#import <shared/shared.h>

#endif /* BridgingHeader_h */

// -------------------------

// iosApp/iosApp/Extensions/SharedExtensions.swift
// Extensions for better Swift interop with Kotlin types

import shared

// MARK: - Flow Extensions

extension Kotlinx_coroutines_coreFlow {
        func observe<T>(onChange: @escaping (T?) -> Void) -> Closeable {
            return FlowUtilsKt.observe(flow: self, onChange: onChange)
        }
}

// MARK: - Room Extensions

extension Room {
        var totalAreaInSquareFeet: Double {
            return totalWallArea * 10.764 // Convert m² to ft²
        }

        var formattedDimensions: String {
            let lengthFt = dimensions.length * 3.281
            let widthFt = dimensions.width * 3.281
            let heightFt = dimensions.height * 3.281
            return String(format: "%.1f × %.1f × %.1f ft", lengthFt, widthFt, heightFt)
        }
}

// MARK: - Surface Extensions

extension Surface {
        var areaInSquareFeet: Double {
            return area * 10.764
        }

        var displayName: String {
            switch type {
                        case .wall:
                        return "Wall \(id.suffix(4))"
                        case .ceiling:
                        return "Ceiling"
                        case .trim:
                        return "Trim"
                        default:
                        return "Surface"
                }
        }
}

// MARK: - Color Extensions

extension ColorInfo {
        var uiColor: UIColor {
            return UIColor(
                    red: CGFloat(rgb.red) / 255.0,
                    green: CGFloat(rgb.green) / 255.0,
                    blue: CGFloat(rgb.blue) / 255.0,
                    alpha: 1.0
            )
        }
}

// MARK: - Paint Extensions

extension Paint {
        var formattedPrice: String {
            return String(format: "$%.2f/gal", pricePerGallon)
        }

        var coverageDescription: String {
            return "\(coverage.sqFtPerGallon) sq ft/gal"
        }
}

// MARK: - Helper Functions

func formatArea(_ squareMeters: Double) -> String {
let squareFeet = squareMeters * 10.764
return String(format: "%.0f sq ft", squareFeet)
}

func formatDimension(_ meters: Double) -> String {
let feet = meters * 3.281
return String(format: "%.1f ft", feet)
}