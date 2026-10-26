package com.shuaib.classmate.activities

/**
 * CoverPageHomeActivity provides the entry point for the Cover Page feature.
 * As requested, the two feature cards (Assignment Cover and Lab Report) are integrated
 * side-by-side directly inside the generator screen rather than requiring a separate navigation hop.
 */
open class CoverPageHomeActivity : CoverPageGeneratorActivity()

/**
 * Backward compatibility alias for existing HomeFragment navigation
 */
class CoverPageActivity : CoverPageGeneratorActivity()
