/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { CheckoutPage } from "./components/CheckoutPage";
import { SuccessPage } from "./components/SuccessPage";
import { FailPage } from "./components/FailPage";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/sandbox" replace />} />
        <Route path="/sandbox" element={<CheckoutPage />} />
        <Route path="/sandbox/success" element={<SuccessPage />} />
        <Route path="/sandbox/fail" element={<FailPage />} />
      </Routes>
    </BrowserRouter>
  );
}
