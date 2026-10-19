#if canImport(Testing)
import Testing
import TracingChrome

@Suite("TracingChrome Swift Export Tests")
struct TracingChromeExportTests {
    @Test("TracingChrome swift module imported cleanly")
    func testSwiftModuleLoads() throws {
        #expect(Bool(true), "TracingChrome swift module imported cleanly")
    }
}
#elseif canImport(XCTest)
import XCTest
import TracingChrome

final class TracingChromeExportTests: XCTestCase {
    func testSwiftModuleLoads() throws {
        XCTAssertTrue(true, "TracingChrome swift module imported cleanly")
    }
}
#endif
