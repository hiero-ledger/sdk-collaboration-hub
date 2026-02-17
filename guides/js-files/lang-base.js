/**
 * Validates that a value is not undefined.
 * This check should be applied to ALL parameters in the public API,
 * even those marked as nullable, since undefined is never acceptable.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is undefined
 * @returns {*} The validated value
 */
export function requireDefined(value, paramName) {
    if (value === undefined) {
        throw new TypeError(`${paramName} must not be undefined`);
    }
    return value;
}

/**
 * Validates that a value is not null or undefined.
 * Use this for non-nullable parameters.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is null or undefined
 */
export function requireNonNull(value, paramName) {
    requireDefined(value, paramName);
    if (value === null) {
        throw new TypeError(`${paramName} must not be null`);
    }
    return value;
}

/**
 * Validates that a value is a non-null string.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is not a string or is null/undefined
 */
export function requireNonNullString(value, paramName) {
    requireNonNull(value, paramName);
    if (typeof value !== 'string') {
        throw new TypeError(`${paramName} must be a string`);
    }
    return value;
}

/**
 * Validates that a value is either a string or null (but not undefined).
 * Use this for parameters marked as @@nullable in the meta-language.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is not a string or null, or if it is undefined
 * @returns {string | null} The validated value
 */
export function requireNullableString(value, paramName) {
    requireDefined(value, paramName);
    if (value !== null && typeof value !== 'string') {
        throw new TypeError(`${paramName} must be a string or null`);
    }
    return value;
}

/**
 * Validates that a value is a non-null number.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is not a number or is null/undefined
 * @returns {number} The validated value
 */
export function requireNonNullNumber(value, paramName) {
    requireNonNull(value, paramName);
    if (typeof value !== 'number' || isNaN(value)) {
        throw new TypeError(`${paramName} must be a number`);
    }
    return value;
}

/**
 * Validates that a value is either a number or null (but not undefined).
 * Use this for nullable numeric parameters.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is not a number or null, or if it is undefined
 * @returns {number | null} The validated value
 */
export function requireNullableNumber(value, paramName) {
    requireDefined(value, paramName);
    if (value !== null && (typeof value !== 'number' || isNaN(value))) {
        throw new TypeError(`${paramName} must be a number or null`);
    }
    return value;
}

/**
 * Validates that a value is a non-null boolean.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is not a boolean or is null/undefined
 * @returns {boolean} The validated value
 */
export function requireNonNullBoolean(value, paramName) {
    requireNonNull(value, paramName);
    if (typeof value !== 'boolean') {
        throw new TypeError(`${paramName} must be a boolean`);
    }
    return value;
}

/**
 * Validates that a value is either a boolean or null (but not undefined).
 * Use this for nullable boolean parameters.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is not a boolean or null, or if it is undefined
 * @returns {boolean | null} The validated value
 */
export function requireNullableBoolean(value, paramName) {
    requireDefined(value, paramName);
    if (value !== null && typeof value !== 'boolean') {
        throw new TypeError(`${paramName} must be a boolean or null`);
    }
    return value;
}

/**
 * Validates that a value is a non-null bigint.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is not a bigint or is null/undefined
 * @returns {bigint} The validated value
 */
export function requireNonNullBigInt(value, paramName) {
    requireNonNull(value, paramName);
    if (typeof value !== 'bigint') {
        throw new TypeError(`${paramName} must be a bigint`);
    }
    return value;
}

/**
 * Validates that a value is either a bigint or null (but not undefined).
 * Use this for nullable bigint parameters.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is not a bigint or null, or if it is undefined
 * @returns {bigint | null} The validated value
 */
export function requireNullableBigInt(value, paramName) {
    requireDefined(value, paramName);
    if (value !== null && typeof value !== 'bigint') {
        throw new TypeError(`${paramName} must be a bigint or null`);
    }
    return value;
}

/**
 * Validates that a value is a non-null Uint8Array.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is not a Uint8Array or is null/undefined
 * @returns {Uint8Array} The validated value
 */
export function requireNonNullUint8Array(value, paramName) {
    requireNonNull(value, paramName);
    if (!(value instanceof Uint8Array)) {
        throw new TypeError(`${paramName} must be a Uint8Array`);
    }
    return value;
}

/**
 * Validates that a value is either a Uint8Array or null (but not undefined).
 * Use this for nullable Uint8Array parameters.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is not a Uint8Array or null, or if it is undefined
 * @returns {Uint8Array | null} The validated value
 */
export function requireNullableUint8Array(value, paramName) {
    requireDefined(value, paramName);
    if (value !== null && !(value instanceof Uint8Array)) {
        throw new TypeError(`${paramName} must be a Uint8Array or null`);
    }
    return value;
}

/**
 * Validates that a value is a non-null Array.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is not an Array or is null/undefined
 * @returns {Array} The validated value
 */
export function requireNonNullArray(value, paramName) {
    requireNonNull(value, paramName);
    if (!Array.isArray(value)) {
        throw new TypeError(`${paramName} must be an Array`);
    }
    return value;
}

/**
 * Validates that a value is either an Array or null (but not undefined).
 * Use this for nullable Array parameters.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is not an Array or null, or if it is undefined
 * @returns {Array | null} The validated value
 */
export function requireNullableArray(value, paramName) {
    requireDefined(value, paramName);
    if (value !== null && !Array.isArray(value)) {
        throw new TypeError(`${paramName} must be an Array or null`);
    }
    return value;
}

/**
 * Validates that a value is a non-null Set.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is not a Set or is null/undefined
 * @returns {Set} The validated value
 */
export function requireNonNullSet(value, paramName) {
    requireNonNull(value, paramName);
    if (!(value instanceof Set)) {
        throw new TypeError(`${paramName} must be a Set`);
    }
    return value;
}

/**
 * Validates that a value is either a Set or null (but not undefined).
 * Use this for nullable Set parameters.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is not a Set or null, or if it is undefined
 * @returns {Set | null} The validated value
 */
export function requireNullableSet(value, paramName) {
    requireDefined(value, paramName);
    if (value !== null && !(value instanceof Set)) {
        throw new TypeError(`${paramName} must be a Set or null`);
    }
    return value;
}

/**
 * Validates that a value is a non-null Map.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is not a Map or is null/undefined
 * @returns {Map} The validated value
 */
export function requireNonNullMap(value, paramName) {
    requireNonNull(value, paramName);
    if (!(value instanceof Map)) {
        throw new TypeError(`${paramName} must be a Map`);
    }
    return value;
}

/**
 * Validates that a value is either a Map or null (but not undefined).
 * Use this for nullable Map parameters.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is not a Map or null, or if it is undefined
 * @returns {Map | null} The validated value
 */
export function requireNullableMap(value, paramName) {
    requireDefined(value, paramName);
    if (value !== null && !(value instanceof Map)) {
        throw new TypeError(`${paramName} must be a Map or null`);
    }
    return value;
}

/**
 * Validates that a value is a non-null, valid Date.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is not a valid Date or is null/undefined
 * @returns {Date} The validated value
 */
export function requireNonNullDate(value, paramName) {
    requireNonNull(value, paramName);
    if (!(value instanceof Date) || isNaN(value.getTime())) {
        throw new TypeError(`${paramName} must be a valid Date`);
    }
    return value;
}

/**
 * Validates that a value is either a valid Date or null (but not undefined).
 * Use this for nullable Date parameters.
 * @param {*} value - The value to check
 * @param {string} paramName - The parameter name for error messages
 * @throws {TypeError} if value is not a valid Date or null, or if it is undefined
 * @returns {Date | null} The validated value
 */
export function requireNullableDate(value, paramName) {
    requireDefined(value, paramName);
    if (value !== null && (!(value instanceof Date) || isNaN(value.getTime()))) {
        throw new TypeError(`${paramName} must be a valid Date or null`);
    }
    return value;
}
